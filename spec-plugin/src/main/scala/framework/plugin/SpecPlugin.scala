// spec-plugin/src/main/scala/framework/plugin/SpecPlugin.scala
package framework.plugin

import sbt._, Keys._
import _root_.framework.spec.SpecCheck
import upickle.default.{write => uwrite}

import java.nio.file.{Files, Path}

/**
  * Custom SBT plugin that aggregates the compile‑time‑generated `.spec` and
  * `.tag` files under `resourceManaged/spec-meta/`, then emits the contractual
  * artefacts:
  *   • **SpecIndex.json** – list[HardwareSpecification]
  *   • **TagIndex.json**  – list[Tag]
  *   • **properties.sva** – SVA assert/cover for the bound properties
  *
  * The aggregation/resolution/report logic lives in [[framework.spec.SpecCheck]]
  * (shared with its CLI); this task is a thin sbt wrapper around it so by-value
  * relations (`@fqn:…`) are resolved identically.
  *
  * ## Usage example (in your design repo `build.sbt`)
  * ```scala
  * enablePlugins(SpecPlugin)
  * Compile / scalacOptions ++= Seq(
  *   s"-Dspec.meta.dir=${(Compile / resourceManaged).value}/spec-meta"
  * )
  * ```
  */
object SpecPlugin extends AutoPlugin {

  /** This plugin only needs the basic JVM plugin (no extra requirements). */
  override def requires = plugins.JvmPlugin

  object autoImport {
    /** Task key: compile → gather meta → emit JSON indices */
    val exportSpecIndex = taskKey[Unit]("Emit SpecIndex.json & TagIndex.json")
  }
  import autoImport._

  /**
    * Project settings contributed by this plugin.  The important bit is the
    * `exportSpecIndex` task that triggers *after* compilation so that all
    * macros/DSL code has already emitted their `.tag` / `.spec` artefacts.
    */
  override lazy val projectSettings: Seq[Setting[_]] = Seq(

    /* --------------------------------------------------------------
     *  Wipe the meta directory before each compile.  The macros emit
     *  `${id}_${uuid}.spec/.tag` files and never overwrite, so without this a
     *  renamed or deleted spec id would leave a stale artefact behind and the
     *  aggregated SpecIndex.json would keep reporting a node that no longer
     *  exists.  Cleaning here guarantees the index reflects only current sources.
     * -------------------------------------------------------------- */
    Compile / compile := (Compile / compile).dependsOn(Def.task {
      IO.delete((Compile / resourceManaged).value / "spec-meta")
    }).value,

    /* --------------------------------------------------------------
     *  Task: compile ▶︎ scan *.spec / *.tag ▶︎ write 2 index files   
     * -------------------------------------------------------------- */
    exportSpecIndex := {
      val log = streams.value.log

      // 0 ──────────────────────────────────────────────────────────────
      // Ensure the latest sources have been compiled.  This guarantees
      // that compile‑time emissions (MetaFile.writeX) have executed.
      // ----------------------------------------------------------------
      (Compile / compile).value

      // 1 ──────────────────────────────────────────────────────────────
      // Locate the metadata directory.  The system property `spec.meta.dir`
      // must have been passed to the macro compiler; build.sbt usually does:
      //   Compile / scalacOptions += s"-Dspec.meta.dir=${(Compile / resourceManaged).value}/spec-meta"
      // ----------------------------------------------------------------
      val metaDir: Path = (Compile / resourceManaged).value.toPath.resolve("spec-meta")
      if (!Files.exists(metaDir)) {
        log.warn(s"[spec-plugin] no spec-meta dir at ${metaDir.toAbsolutePath} — nothing to index")
      } else {
        log.info(s"[spec-plugin] scanning meta artefacts in ${metaDir.toAbsolutePath}")

        // Aggregate + resolve by-value (`@fqn:…`) relations via the shared
        // SpecCheck logic, so the plugin and the CLI produce identical output.
        val specs  = SpecCheck.resolveFqns(SpecCheck.loadSpecs(metaDir))
        val tags   = SpecCheck.loadTags(metaDir)
        val report = SpecCheck.Report(specs, tags)

        val outDir = (Compile / target).value
        IO.write(outDir / "SpecIndex.json", uwrite(specs, indent = 2))
        IO.write(outDir / "TagIndex.json",  uwrite(tags,  indent = 2))
        IO.write(outDir / "properties.sva", report.sva("clk", "reset"))

        // Surface the compliance report in the build log.
        report.render.split("\n").foreach(l => log.info(l))
        log.success(s"SpecIndex / TagIndex / properties.sva → ${outDir.getAbsolutePath}")
        if (report.hardViolations > 0)
          log.error(s"[spec-plugin] ${report.hardViolations} dangling reference(s) — see report above")
      }
    },

    /* --------------------------------------------------------------
     *  Make the test task depend on up‑to‑date JSON indices
     * -------------------------------------------------------------- */
    (Test / test) := (Test / test).dependsOn(exportSpecIndex).value,
  )
}
