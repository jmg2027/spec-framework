// spec-core/src/main/scala/framework/spec/SpecGen.scala
// -----------------------------------------------------------------------------
//  SpecGen — generate a Chisel *design skeleton* FROM the spec graph.
//
//  The spec is the single source of truth, so the correct direction is
//  spec → design: from the aggregated SpecIndex (parameters, typed bundles with
//  widths, interfaces with directions, contracts and their relations, properties)
//  emit a compilable Chisel skeleton with every spec already wired in
//  (config case class, bundle classes, modules with `localSpec`-anchored ports,
//  sub-module instances, and `assertProperty` stubs). The engineer fills in the
//  logic where `??? / DontCare / true.B` placeholders are.
//
//  Pure text generation — no Chisel dependency. Run:  SpecGen <meta-dir> <out-dir>
// -----------------------------------------------------------------------------
package framework.spec

import java.nio.file.{Files, Path, Paths}

object SpecGen {

  // ---- naming: spec id → Scala identifier --------------------------------
  private def stripPrefix(id: String): String = id.split("_", 2) match {
    case Array(_, rest) => rest
    case _              => id
  }
  private def pascal(id: String): String =
    stripPrefix(id).split("_").filter(_.nonEmpty).map(_.toLowerCase.capitalize).mkString
  private def camel(id: String): String = {
    val p = pascal(id); if (p.isEmpty) p else p.head.toLower + p.tail
  }

  // ---- field type label → Chisel ----------------------------------------
  private val uintRe = """UInt\((\d+)\)""".r
  private def chiselType(label: String): String = label match {
    case uintRe(w)                 => s"UInt($w.W)"
    case "UInt"                    => "UInt(0.W) /* TODO width */"
    case l if l.startsWith("Bool") => "Bool()"
    case l                         => s"new ${l.replaceAll("""\(\d+\)""", "")}(c)" // a nested bundle
  }

  // ---- generators --------------------------------------------------------
  def genConfig(params: List[HardwareSpecification], name: String = "Config"): String = {
    val fields = params.flatMap { p =>
      val m = p.lists.toMap
      for (n <- m.get("name")) yield {
        val t = m.getOrElse("type", "Int")
        val d = m.get("default").map(v => s" = $v").getOrElse("")
        s"  $n: $t$d,  // ${p.id}"
      }
    }
    s"""final case class $name(
       |${fields.mkString("\n")}
       |)
       |""".stripMargin
  }

  def genBundle(b: HardwareSpecification): String = {
    val fields = b.lists.map { case (n, t) => s"  val $n = ${chiselType(t)}" }
    s"""/** ${b.description.replaceAll("\\s+", " ").trim} (${b.id}) */
       |class ${pascal(b.id)}(c: Config) extends Bundle {
       |${fields.mkString("\n")}
       |}
       |""".stripMargin
  }

  def genModule(cont: HardwareSpecification, byId: Map[String, HardwareSpecification]): String = {
    def cat(id: String) = byId.get(id).map(_.category)
    val has  = cont.has.toList.sorted
    val intfs = has.filter(cat(_).contains(SpecCategory.INTERFACE)).map(byId)
    val subs  = has.filter(cat(_).contains(SpecCategory.CONTRACT)).map(byId)
    val funcs = has.filter(cat(_).contains(SpecCategory.FUNCTION)).map(byId)
    val props = (cont.has ++ cont.is).toList.flatMap(byId.get).filter(_.category == SpecCategory.PROPERTY)

    def port(i: HardwareSpecification): String = {
      val m     = i.lists.toMap
      val input = m.get("direction").exists(_.contains("Flipped")) || i.id.endsWith("_IN")
      val bnd   = i.has.toList.flatMap(byId.get).find(_.category == SpecCategory.BUNDLE).map(b => pascal(b.id)).getOrElse("UInt /*?*/")
      val tpe   = if (input) s"Flipped(Decoupled(new $bnd(c)))" else s"Decoupled(new $bnd(c))"
      s"  val ${camel(i.id)} = localSpec(${valName(i.id)}, IO($tpe))"
    }
    val ports   = intfs.map(port)
    val subInst = subs.map(s => s"  val ${camel(s.id)} = Module(new ${pascal(s.id)}(c))")
    val funcStubs = funcs.map(f => s"  localSpec(${valName(f.id)})  // ${f.description.replaceAll("\\s+"," ").trim.take(70)}")
    val propStubs = props.map(p => s"""  assert(assertProperty(${valName(p.id)}) { true.B /* TODO: ${p.description.replaceAll("\\s+"," ").trim.take(60)} */ }, "${p.id}")""")

    s"""/** ${cont.description.replaceAll("\\s+", " ").trim} (${cont.id}) */
       |class ${pascal(cont.id)}(c: Config) extends Module {
       |  localSpec(${valName(cont.id)})
       |
       |${ports.mkString("\n")}
       |${if (subInst.nonEmpty) "\n" + subInst.mkString("\n") + "\n" else ""}
       |${funcStubs.mkString("\n")}
       |
       |  // TODO: pipeline logic; drive outputs (use DontCare to start)
       |${propStubs.mkString("\n")}
       |}
       |""".stripMargin
  }

  /** Spec val name for a spec id, matching the mirrored specs/ naming convention
    * (CONT_FETCH_UNIT → contFetchUnit, INTF_FOO → intfFoo, …). */
  private def valName(id: String): String = {
    val pre = id.split("_", 2).head match {
      case "CONT" => "cont"; case "INTF" => "intf"; case "FUNC" => "func"
      case "PROP" => "prop"; case "COV" => "cov"; case "BND" => "bnd"; case "PARAM" => "param"
      case _      => ""
    }
    pre + pascal(id)
  }

  def main(args: Array[String]): Unit = {
    val pos     = args.filterNot(_.startsWith("--"))
    val metaArg = pos.headOption.orElse(sys.props.get("spec.meta.dir")).getOrElse {
      System.err.println("usage: SpecGen <meta-dir> [<out-dir>]"); sys.exit(2)
    }
    val metaDir = Paths.get(metaArg)
    val outDir  = Paths.get(pos.lift(1).getOrElse(metaArg))
    Files.createDirectories(outDir)

    val specs = SpecCheck.applyWidths(SpecCheck.resolveFqns(SpecCheck.loadSpecs(metaDir)), SpecCheck.loadWidths(metaDir))
    val byId  = specs.map(s => s.id -> s).toMap
    def of(c: SpecCategory) = specs.filter(_.category == c).sortBy(_.id)

    val sb = new StringBuilder
    sb.append("// GENERATED skeleton from the spec graph by SpecGen. Fill in the logic.\n")
    sb.append("import chisel3._\nimport chisel3.util._\nimport framework.macros.localSpec\nimport framework.macros.Formal._\n")
    // import the spec objects (derived from each spec's scalaDeclarationPath) so the
    // localSpec/assertProperty references resolve.
    val specObjects = specs.flatMap { s =>
      val p = s.scalaDeclarationPath
      if (p.contains(".")) Some(p.substring(0, p.lastIndexOf('.'))) else None
    }.distinct.sorted
    specObjects.foreach(o => sb.append(s"import $o._\n"))
    sb.append("\n")
    sb.append(genConfig(of(SpecCategory.PARAMETER))).append("\n")
    of(SpecCategory.BUNDLE).foreach(b => sb.append(genBundle(b)).append("\n"))
    of(SpecCategory.CONTRACT).foreach(c => sb.append(genModule(c, byId)).append("\n"))

    val f = outDir.resolve("Generated.scala")
    Files.write(f, sb.toString.getBytes)
    println(s"[SpecGen] ${of(SpecCategory.CONTRACT).size} modules, ${of(SpecCategory.BUNDLE).size} bundles, " +
            s"${of(SpecCategory.PARAMETER).size} params → ${f.toAbsolutePath}")
  }
}
