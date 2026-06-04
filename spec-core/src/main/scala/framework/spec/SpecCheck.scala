// spec-core/src/main/scala/framework/spec/SpecCheck.scala
// -----------------------------------------------------------------------------
//  SpecCheck — aggregate the compile-time `.spec`/`.tag` artefacts, emit the
//  SpecIndex.json / TagIndex.json indices, and run a spec-compliance report.
//
//  This is the consumer side of the framework: it turns the structured spec
//  graph + the source-anchored bindings into actionable signal. Several classes
//  of spec↔implementation drift that the *compiler* cannot catch (because they
//  are about completeness / coverage, not types) are surfaced here:
//
//    • dangling references  — `is/has/uses` pointing at an undefined spec id
//    • implementation gaps  — CONTRACT/INTERFACE/FUNCTION nodes with no @LocalSpec
//    • formal coverage      — PROPERTY/COVERAGE nodes with no assert/cover binding
//    • bundle incompleteness— typed bundles with public fields left undeclared
//
//  Field-level type drift is intentionally absent: `bundleSpec[T]` makes that a
//  *compile error*, so it can never reach this stage.
//
//  Usage:  SpecCheck <meta-dir> [<out-dir>]
//          exit code 0 = clean, 1 = hard violations (dangling refs).
// -----------------------------------------------------------------------------
package framework.spec

import java.nio.file.{Files, Path, Paths}
// scala.collection.JavaConverters is the one converters API present on 2.12, 2.13
// AND 3 (scala.jdk.CollectionConverters does not exist on 2.12). Deprecated on
// 2.13+/3 but kept for cross-version source compatibility.
import scala.collection.JavaConverters._
import upickle.default.{read => uread, write => uwrite}

object SpecCheck {

  // ---- Aggregation --------------------------------------------------------

  def loadSpecs(metaDir: Path): List[HardwareSpecification] =
    Files.walk(metaDir).iterator.asScala
      .filter(_.toString.endsWith(".spec"))
      .flatMap { p =>
        val lines     = Files.readAllLines(p).asScala.toList
        val jsonStart = lines.indexWhere(_.trim.startsWith("{"))
        if (jsonStart < 0) None
        else
          try Some(uread[HardwareSpecification](lines.drop(jsonStart).mkString("\n")))
          catch { case _: Throwable => None }
      }
      .toList
      // de-dupe by id, preferring the copy that carries a scalaDeclarationPath
      .groupBy(_.id)
      .map { case (_, dup) => dup.find(_.scalaDeclarationPath.nonEmpty).getOrElse(dup.head) }
      .toList

  /**
   * Resolve `@fqn:<declarationPath>` relation references (emitted by the `spec`
   * macro for by-value relations) back to spec ids, using the
   * scalaDeclarationPath → id map. Anything that still does not resolve is left
   * as-is so the dangling-reference check can flag it.
   */
  def resolveFqns(specs: List[HardwareSpecification]): List[HardwareSpecification] = {
    val pathToId = specs.collect { case s if s.scalaDeclarationPath.nonEmpty => s.scalaDeclarationPath -> s.id }.toMap
    def res(refs: Set[String]): Set[String] = refs.map { r =>
      if (r.startsWith("@fqn:")) pathToId.getOrElse(r.drop(5), r) else r
    }
    specs.map(s => s.copy(is = res(s.is), has = res(s.has), uses = res(s.uses)))
  }

  /**
   * Resolve `@fqn:` tag ids (emitted on Scala 3, where `@LocalSpec` /
   * `assertProperty` cannot look up the spec id at compile time because specs are
   * emitted at run time) back to real spec ids via the declaration-path map.
   * On Scala 2 tags already carry real ids, so this is a no-op there.
   */
  def resolveTagFqns(tags: List[Tag], specs: List[HardwareSpecification]): List[Tag] = {
    val pathToId = specs.collect { case s if s.scalaDeclarationPath.nonEmpty => s.scalaDeclarationPath -> s.id }.toMap
    tags.map(t => if (t.id.startsWith("@fqn:")) t.copy(id = pathToId.getOrElse(t.id.drop(5), t.id)) else t)
  }

  def loadTags(metaDir: Path): List[Tag] =
    Files.walk(metaDir).iterator.asScala
      .filter(_.toString.endsWith(".tag"))
      .flatMap { p =>
        try Some(uread[Tag](Files.readString(p)))
        catch { case _: Throwable => None }
      }
      .toList

  /** Load VerifIndex.json (sim/cover results) if a run produced one. */
  def loadVerif(metaDir: Path): List[VerifResult] = {
    val f = metaDir.resolve("VerifIndex.json")
    if (!Files.exists(f)) Nil
    else try uread[List[VerifResult]](Files.readString(f)) catch { case _: Throwable => Nil }
  }

  // ---- Report model -------------------------------------------------------

  final case class Report(
      specs: List[HardwareSpecification],
      tags: List[Tag],
      verif: List[VerifResult] = Nil,
  ) {
    private val ids       = specs.map(_.id).toSet
    private val byCat     = specs.groupBy(_.category)
    private val implIds   = tags.filter(_.kind == "impl").map(_.id).toSet
    private val assertIds = tags.filter(t => t.kind == "assert" || t.kind == "cover").map(_.id).toSet
    private val verifById = verif.map(v => v.id -> v).toMap

    /** is/has/uses references that point at an undefined spec id. */
    val dangling: List[(String, String, String)] =
      for {
        s   <- specs
        (rel, ref) <- s.is.toList.map(("is", _)) ::: s.has.toList.map(("has", _)) ::: s.uses.toList.map(("uses", _))
        if !ids.contains(ref)
      } yield (s.id, rel, ref)

    private def cat(c: SpecCategory): List[HardwareSpecification] = byCat.getOrElse(c, Nil)

    /** Structural nodes that should be anchored to RTL but carry no @LocalSpec. */
    val unimplemented: List[HardwareSpecification] =
      (cat(SpecCategory.CONTRACT) ::: cat(SpecCategory.INTERFACE) ::: cat(SpecCategory.FUNCTION))
        .filterNot(s => implIds.contains(s.id))
        .sortBy(_.id)

    /** PROPERTY/COVERAGE nodes with no bound assertion/cover. */
    val unenforced: List[HardwareSpecification] =
      (cat(SpecCategory.PROPERTY) ::: cat(SpecCategory.COVERAGE))
        .filterNot(s => assertIds.contains(s.id))
        .sortBy(_.id)

    /** Typed bundles that left public implementation fields undeclared. */
    val incompleteBundles: List[(String, String)] =
      cat(SpecCategory.BUNDLE).flatMap { s =>
        s.notes.find(_.startsWith("undeclared-fields:")).map(n => s.id -> n.stripPrefix("undeclared-fields:").trim)
      }.sortBy(_._1)

    def hardViolations: Int = dangling.size

    /**
     * Render the bound properties as a SystemVerilog assertions snippet — the
     * concrete formal artifact the "formal 연결" feeds. Bound PROPERTY/COVERAGE
     * nodes become `assert/cover property`; unbound ones become TODO stubs so the
     * formal gap is visible in the generated file too.
     */
    def sva(clock: String, reset: String): String = {
      val descOf  = specs.map(s => s.id -> s.description.replaceAll("\\s+", " ").trim).toMap
      val bound   = tags.filter(t => t.kind == "assert" || t.kind == "cover").sortBy(_.id)
      val sb = new StringBuilder
      sb.append("// ───────────────────────────────────────────────────────────────────────\n")
      sb.append("// properties.sva — GENERATED from the spec graph by SpecCheck. Do not edit.\n")
      sb.append("//   bound PROPERTY  ⇒ assert property      bound COVERAGE ⇒ cover property\n")
      sb.append("//   unbound PROPERTY/COVERAGE ⇒ TODO stub (declared but not enforced)\n")
      sb.append(s"//   clock = $clock   reset = $reset   (override with --clock=… / --reset=…)\n")
      sb.append("// ───────────────────────────────────────────────────────────────────────\n")
      sb.append("`ifndef SPEC_PROPERTIES_SVH\n`define SPEC_PROPERTIES_SVH\n\n")
      bound.foreach { t =>
        val kw     = if (t.kind == "assert") "assert" else "cover"
        val prefix = if (t.kind == "assert") "ap" else "cp"
        sb.append(s"// ${t.id} — ${descOf.getOrElse(t.id, "")}\n")
        sb.append(s"//   bound at ${shortSrc(t.srcFile)}:${t.line}\n")
        sb.append(s"${prefix}_${t.id}: $kw property (@(posedge $clock) disable iff ($reset)\n")
        sb.append(s"    (${t.expr}));\n\n")
      }
      if (unenforced.nonEmpty) {
        sb.append("// ── unenforced (no binding found) ──────────────────────────────────────\n")
        unenforced.foreach(s => sb.append(s"// TODO ${s.id} — ${descOf.getOrElse(s.id, "")}\n"))
        sb.append("\n")
      }
      sb.append("`endif\n")
      sb.toString
    }

    /**
     * A SymbiYosys script that runs the bound PROPERTY assertions formally
     * against the elaborated Verilog. It is scaffolding — `sby spec_formal.sby`
     * proves them once yosys + a solver are installed; the conditions in
     * `properties.sva` reference design signals, so a one-line `bind` may be
     * needed for hierarchical paths.
     */
    def formalSby(top: String, clock: String, reset: String): String = {
      val asserts = tags.count(_.kind == "assert")
      val covers  = tags.count(_.kind == "cover")
      s"""# spec_formal.sby — GENERATED by SpecCheck. `sby spec_formal.sby` (needs yosys).
         |# Proves $asserts asserted propert${if (asserts == 1) "y" else "ies"} and exercises $covers cover point(s)
         |# from the spec graph against the elaborated design ($top).
         |[options]
         |mode prove
         |depth 20
         |
         |[engines]
         |smtbmc
         |
         |[script]
         |read -formal $top.sv
         |read -formal properties.sva
         |prep -top $top
         |
         |[files]
         |$top.sv
         |properties.sva
         |""".stripMargin
    }

    /**
     * Functional coverage **auto-derived from the spec graph** — one obligation
     * per INTERFACE (it should fire) and per FUNCTION (it should be exercised).
     * No coverage plan is hand-written: writing the FUNCTION/INTERFACE specs (for
     * the design) is enough. Emitted as `cover property` stubs to fill the signal.
     */
    def functionalSva: String = {
      val sb = new StringBuilder
      def one(s: HardwareSpecification, what: String, hint: String): Unit = {
        sb.append(s"// ${s.id} — ${s.description.replaceAll("\\s+", " ").trim}\n")
        sb.append(s"cf_${s.id}: cover property (@(posedge clk) disable iff (reset) $hint); // $what\n\n")
      }
      sb.append("// functional_coverage.sva — auto-derived from the spec graph by SpecCheck.\n")
      sb.append("// One cover per INTERFACE (fires) and FUNCTION (exercised). Fill the signal hints.\n\n")
      (cat(SpecCategory.INTERFACE)).sortBy(_.id).foreach(s => one(s, "interface fired", s"/* ${s.id}.valid && ${s.id}.ready */"))
      (cat(SpecCategory.FUNCTION)).sortBy(_.id).foreach(s => one(s, "function exercised", s"/* <${s.id} active> */"))
      sb.toString
    }

    /**
     * Render the whole spec graph as a self-contained, GitHub-friendly Markdown
     * document: a coverage summary, a Mermaid diagram of the graph (renders with
     * no extra tooling), and a per-node section with descriptions, relations as
     * links, bundle field tables, RTL anchors and formal bindings.
     */
    def markdown: String = {
      val sb = new StringBuilder
      def anchor(id: String): String = id.toLowerCase
      def link(ref: String): String  = if (ids.contains(ref)) s"[$ref](#${anchor(ref)})" else s"`$ref` ⚠️"
      def oneLine(s: String): String  = s.split("\n").map(_.trim).filter(_.nonEmpty).mkString(" ")
      val implBy   = tags.filter(_.kind == "impl").groupBy(_.id)
      val formalBy = tags.filter(t => t.kind == "assert" || t.kind == "cover").groupBy(_.id)
      val structural = cat(SpecCategory.CONTRACT).size + cat(SpecCategory.INTERFACE).size + cat(SpecCategory.FUNCTION).size
      val props      = cat(SpecCategory.PROPERTY).size + cat(SpecCategory.COVERAGE).size
      def badge(ok: Boolean): String = if (ok) "✅" else "⚠️"

      sb.append("# Spec Report\n\n")
      sb.append(s"> ${specs.size} specs · ${tags.size} bindings · generated by `SpecCheck`\n\n")

      // -- coverage --
      sb.append("## Coverage\n\n| metric | value |\n|---|---|\n")
      sb.append(s"| Implementation (structural ↔ tag) | ${structural - unimplemented.size} / $structural ${badge(unimplemented.isEmpty)} |\n")
      sb.append(s"| Formal (property ↔ check) | ${props - unenforced.size} / $props ${badge(unenforced.isEmpty)} |\n")
      sb.append(s"| Typed bundle completeness | ${cat(SpecCategory.BUNDLE).size - incompleteBundles.size} / ${cat(SpecCategory.BUNDLE).size} ${badge(incompleteBundles.isEmpty)} |\n")
      sb.append(s"| Dangling references | ${dangling.size} ${badge(dangling.isEmpty)} |\n")
      if (verif.nonEmpty) {
        val checkable = cat(SpecCategory.PROPERTY).size + cat(SpecCategory.COVERAGE).size
        val passed    = verif.count(v => v.status == "passed" || v.status == "covered")
        val failed    = verif.count(_.status == "failed")
        sb.append(s"| Verified in sim (passed/covered) | $passed / $checkable ${badge(failed == 0 && passed > 0)} |\n")
      }
      if (verif.exists(_.kind == "functional")) {
        val obl = cat(SpecCategory.FUNCTION).size + cat(SpecCategory.INTERFACE).size
        val hit = (cat(SpecCategory.FUNCTION) ::: cat(SpecCategory.INTERFACE)).count(s => verifById.get(s.id).exists(_.hits > 0))
        sb.append(s"| Functional coverage (auto-derived) | $hit / $obl ${badge(hit == obl)} |\n")
      }
      sb.append("\n")

      // -- mermaid graph --
      sb.append("## Graph\n\n```mermaid\ngraph LR\n")
      specs.sortBy(_.id).foreach { s =>
        sb.append(s"""  ${s.id}["${s.id}<br/><small>${catName(s.category)}</small>"]:::${classOf(s.category)}\n""")
      }
      specs.sortBy(_.id).foreach { s =>
        val edges =
          s.is.toList.map(("is", _)) ::: s.has.toList.map(("has", _)) ::: s.uses.toList.map(("uses", _))
        edges.filter { case (_, r) => ids.contains(r) }.sortBy(_._2).foreach { case (rel, r) =>
          sb.append(s"  ${s.id} -->|$rel| $r\n")
        }
      }
      List(
        SpecCategory.CONTRACT  -> "fill:#dbeafe,stroke:#2563eb",
        SpecCategory.INTERFACE -> "fill:#dcfce7,stroke:#16a34a",
        SpecCategory.FUNCTION  -> "fill:#fef9c3,stroke:#ca8a04",
        SpecCategory.BUNDLE    -> "fill:#f3e8ff,stroke:#9333ea",
        SpecCategory.PARAMETER -> "fill:#f1f5f9,stroke:#64748b",
        SpecCategory.PROPERTY  -> "fill:#ffe4e6,stroke:#e11d48",
        SpecCategory.COVERAGE  -> "fill:#ffedd5,stroke:#ea580c",
      ).foreach { case (c, style) => sb.append(s"  classDef ${classOf(c)} $style\n") }
      sb.append("```\n\n")

      // -- per-node detail --
      byCat.toList.sortBy(_._1.toString).foreach { case (c, xs) =>
        sb.append(s"## ${catName(c)}\n\n")
        xs.sortBy(_.id).foreach { s =>
          sb.append(s"### ${s.id}\n\n")
          if (s.description.nonEmpty) sb.append(oneLine(s.description)).append("\n\n")
          s.status.foreach(st => sb.append(s"**Status:** `$st`\n\n"))

          def relRow(label: String, refs: Set[String]): Unit =
            if (refs.nonEmpty) sb.append(s"- **$label** → ${refs.toList.sorted.map(link).mkString(", ")}\n")
          relRow("is", s.is); relRow("has", s.has); relRow("uses", s.uses)
          if (s.is.nonEmpty || s.has.nonEmpty || s.uses.nonEmpty) sb.append("\n")

          if (s.lists.nonEmpty) {
            val header = if (s.category == SpecCategory.BUNDLE) ("Field", "Type") else ("Key", "Value")
            sb.append(s"| ${header._1} | ${header._2} |\n|---|---|\n")
            s.lists.foreach { case (k, v) => sb.append(s"| $k | $v |\n") }
            sb.append("\n")
          }

          implBy.getOrElse(s.id, Nil).foreach(t => sb.append(s"- 🔗 **RTL:** `${shortSrc(t.srcFile)}:${t.line}`\n"))
          formalBy.getOrElse(s.id, Nil).foreach { t =>
            val k = if (t.kind == "assert") "asserted" else "covered"
            sb.append(s"- ✅ **$k:** `${t.expr}` _(${shortSrc(t.srcFile)}:${t.line})_\n")
          }
          if ((s.category == SpecCategory.CONTRACT || s.category == SpecCategory.INTERFACE || s.category == SpecCategory.FUNCTION)
              && !implBy.contains(s.id))
            sb.append("- ⚠️ **no RTL anchor**\n")
          if ((s.category == SpecCategory.PROPERTY || s.category == SpecCategory.COVERAGE) && !formalBy.contains(s.id))
            sb.append("- ⚠️ **not enforced by any check**\n")
          verifById.get(s.id).foreach { v =>
            val m = v.status match {
              case "passed"    => s"🧪 **sim:** passed — held for all ${v.cycles} cycles"
              case "failed"    => s"🧪 **sim:** ❌ FAILED at cycle ${v.firstFailCycle} (${v.fails}/${v.cycles} cycles)"
              case "covered"   => s"📊 **cover:** hit ${v.hits} time(s) in ${v.cycles} cycles"
              case "uncovered" => s"📊 **cover:** ⚠️ never hit in ${v.cycles} cycles"
              case other       => other
            }
            sb.append(s"- $m\n")
          }

          s.notes.filterNot(_ == "typed-bundle").foreach(n => sb.append(s"> _${n}_\n"))
          (s.tables ::: s.drawings ::: s.codes).foreach(block => sb.append("\n").append(block).append("\n"))
          sb.append("\n")
        }
      }
      sb.toString
    }

    def render: String = {
      val sb = new StringBuilder
      def h(t: String): Unit = { sb.append("\n").append(t).append("\n").append("─" * t.length).append("\n") }

      sb.append("══════════════════════════════════════════════════════\n")
      sb.append("  SPEC COMPLIANCE REPORT\n")
      sb.append("══════════════════════════════════════════════════════\n")

      h("Spec graph")
      byCat.toList.sortBy(_._1.toString).foreach { case (c, xs) =>
        sb.append(f"  ${catName(c)}%-12s ${xs.size}%3d\n")
      }
      sb.append(f"  ${"TOTAL"}%-12s ${specs.size}%3d nodes, ${tags.size} bindings\n")

      h("Implementation coverage (structural nodes ↔ @LocalSpec)")
      val structural = cat(SpecCategory.CONTRACT).size + cat(SpecCategory.INTERFACE).size + cat(SpecCategory.FUNCTION).size
      sb.append(f"  bound:   ${structural - unimplemented.size}%3d / $structural%-3d\n")
      if (unimplemented.nonEmpty) {
        sb.append("  UNIMPLEMENTED (no RTL anchor):\n")
        unimplemented.foreach(s => sb.append(s"    ⚠ ${s.id}  [${catName(s.category)}]\n"))
      } else sb.append("  ✓ every structural node is anchored to RTL\n")

      h("Formal coverage (PROPERTY/COVERAGE ↔ assert/cover)")
      val props = cat(SpecCategory.PROPERTY).size + cat(SpecCategory.COVERAGE).size
      sb.append(f"  enforced: ${props - unenforced.size}%3d / $props%-3d\n")
      if (unenforced.nonEmpty) {
        sb.append("  UNENFORCED (declared but not bound to any check):\n")
        unenforced.foreach(s => sb.append(s"    ⚠ ${s.id}  [${catName(s.category)}]\n"))
      } else sb.append("  ✓ every property/coverage node is bound to a check\n")
      val bound = tags.filter(t => t.kind == "assert" || t.kind == "cover")
      if (bound.nonEmpty) {
        sb.append("  bindings:\n")
        bound.sortBy(_.id).foreach(t =>
          sb.append(s"    • ${t.id}  ⇐  ${t.expr}   (${shortSrc(t.srcFile)}:${t.line})\n"))
      }

      if (verif.nonEmpty) {
        val backends = verif.map(_.backend).distinct.sorted.mkString(", ")
        h(s"Verification (spec-driven, backend: $backends)")
        val checkable = (cat(SpecCategory.PROPERTY) ::: cat(SpecCategory.COVERAGE))
        val exercised = checkable.count(s => verifById.contains(s.id))
        sb.append(f"  exercised: $exercised%3d / ${checkable.size}%-3d   (${verif.map(_.cycles).reduceOption(_ max _).getOrElse(0)} cycles)\n")
        checkable.sortBy(_.id).foreach { s =>
          val mark = verifById.get(s.id).map(v => v.status match {
            case "passed"    => s"✓ passed (${v.cycles} cy)"
            case "failed"    => s"✗ FAILED @cycle ${v.firstFailCycle} (${v.fails}/${v.cycles})"
            case "covered"   => s"✓ covered (${v.hits} hits)"
            case "uncovered" => s"○ uncovered (0/${v.cycles})"
            case other       => other
          }).getOrElse("· not exercised in sim")
          sb.append(f"    ${s.id}%-28s ").append(mark).append("\n")
        }
      }

      if (verif.exists(_.kind == "functional")) {
        h("Functional coverage (auto-derived from FUNCTION/INTERFACE specs)")
        val obligations = cat(SpecCategory.FUNCTION) ::: cat(SpecCategory.INTERFACE)
        val hit         = obligations.count(s => verifById.get(s.id).exists(_.hits > 0))
        sb.append(f"  exercised: $hit%3d / ${obligations.size}%-3d   (no coverage plan hand-written — derived from the spec graph)\n")
        val missed = obligations.filterNot(s => verifById.get(s.id).exists(_.hits > 0)).sortBy(_.id)
        if (missed.nonEmpty) {
          sb.append("  NOT EXERCISED:\n")
          missed.foreach(s => sb.append(s"    ○ ${s.id}  [${catName(s.category)}]\n"))
        } else sb.append("  ✓ every interface fired and every function ran\n")
      }

      h("Typed bundle completeness")
      if (incompleteBundles.nonEmpty)
        incompleteBundles.foreach { case (id, fs) => sb.append(s"    ⚠ $id leaves undeclared: $fs\n") }
      else sb.append("  ✓ all typed bundles declare every public field\n")

      h("Dangling references (HARD)")
      if (dangling.nonEmpty)
        dangling.sortBy(_._1).foreach { case (from, rel, ref) =>
          val shown = if (ref.startsWith("@fqn:")) ref.drop(5) + " (unresolved ref)" else ref
          sb.append(s"    ✗ $from --$rel--> $shown   (undefined)\n")
        }
      else sb.append("  ✓ every reference resolves to a defined spec\n")

      sb.append("\n══════════════════════════════════════════════════════\n")
      sb.append(
        if (hardViolations == 0) "  RESULT: PASS (no hard violations)\n"
        else s"  RESULT: FAIL ($hardViolations dangling reference(s))\n")
      sb.append("══════════════════════════════════════════════════════\n")
      sb.toString
    }
  }

  private def catName(c: SpecCategory): String = c match {
    case SpecCategory.RAW(p) => s"RAW:$p"
    case other               => other.toString.replaceFirst("^.*\\$", "")
  }
  /** A Mermaid-safe class name derived from the category. */
  private def classOf(c: SpecCategory): String = "cat" + catName(c).replaceAll("[^A-Za-z0-9]", "")
  private def shortSrc(s: String): String = s.split("/").lastOption.getOrElse(s)

  // ---- Entry point --------------------------------------------------------

  def main(args: Array[String]): Unit = {
    val flags = args.filter(_.startsWith("--"))
    val pos   = args.filterNot(_.startsWith("--"))
    def flagVal(name: String, default: String): String =
      flags.collectFirst { case f if f.startsWith(s"--$name=") => f.drop(name.length + 3) }.getOrElse(default)

    // meta-dir: first positional arg, else the spec.meta.dir system property.
    val metaArg = pos.headOption.orElse(sys.props.get("spec.meta.dir"))
    if (metaArg.isEmpty) {
      System.err.println("usage: SpecCheck <meta-dir> [<out-dir>] [--clock=NAME] [--reset=NAME] [--strict]")
      System.err.println("  (meta-dir may also come from the spec.meta.dir system property)")
      System.err.println("  --strict : exit non-zero on warnings too (unimplemented / unenforced / incomplete)")
      sys.exit(2)
    }
    val metaDir = Paths.get(metaArg.get)
    if (!Files.isDirectory(metaDir)) {
      System.err.println(s"[SpecCheck] meta dir not found: $metaDir")
      sys.exit(2)
    }
    val clock  = flagVal("clock", "clk")
    val reset  = flagVal("reset", "reset")
    val top    = flagVal("top", "Top")
    val strict = flags.contains("--strict")

    val specs = resolveFqns(loadSpecs(metaDir))
    val tags  = resolveTagFqns(loadTags(metaDir), specs)
    val verif = loadVerif(metaDir)

    val outDir = Paths.get(pos.lift(1).getOrElse(metaArg.get))
    Files.createDirectories(outDir)
    val report = Report(specs, tags, verif)
    Files.write(outDir.resolve("SpecIndex.json"), uwrite(specs, indent = 2).getBytes)
    Files.write(outDir.resolve("TagIndex.json"), uwrite(tags, indent = 2).getBytes)
    Files.write(outDir.resolve("properties.sva"), report.sva(clock, reset).getBytes)
    Files.write(outDir.resolve("spec_formal.sby"), report.formalSby(top, clock, reset).getBytes)
    Files.write(outDir.resolve("functional_coverage.sva"), report.functionalSva.getBytes)
    Files.write(outDir.resolve("SPEC.md"), report.markdown.getBytes)

    print(report.render)
    println(s"[SpecCheck] SpecIndex.json / TagIndex.json / properties.sva / spec_formal.sby / SPEC.md → ${outDir.toAbsolutePath}")

    // Exit policy: dangling refs always fail; --strict also fails on the soft
    // warnings (useful as a CI gate once a project intends full coverage).
    val softCount = report.unimplemented.size + report.unenforced.size + report.incompleteBundles.size
    val failed    = report.hardViolations > 0 || (strict && softCount > 0)
    if (strict)
      println(s"[SpecCheck] strict mode: ${report.hardViolations} hard + $softCount soft → ${if (failed) "FAIL" else "PASS"}")
    sys.exit(if (failed) 1 else 0)
  }
}
