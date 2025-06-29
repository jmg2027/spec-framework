// spec-plugin/src/main/scala/framework/plugin/SpecPlugin.scala
package framework.plugin

import sbt._, Keys._
import _root_.framework.spec.{HardwareSpecification, Tag}
import upickle.default.{read => uread, write => uwrite}

import java.nio.file.{Files, Path}
import scala.collection.JavaConverters._

/**
  * Custom SBT plugin that aggregates the compile‑time‑generated `.spec` and
  * `.tag` files under `resourceManaged/spec-meta/`, then emits two contractual
  * JSON indices:
  *   • **SpecIndex.json**   – list[HardwareSpecification]
  *   • **ModuleIndex.json** – list[Tag]
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
    val exportSpecIndex = taskKey[Unit]("Emit SpecIndex.json & ModuleIndex.json")
  }
  import autoImport._

  /**
    * Project settings contributed by this plugin.  The important bit is the
    * `exportSpecIndex` task that triggers *after* compilation so that all
    * macros/DSL code has already emitted their `.tag` / `.spec` artefacts.
    */
  override lazy val projectSettings: Seq[Setting[_]] = Seq(

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

        // Helper lambdas for file discovery; keeps the fold below readable
        def isSpec(p: Path): Boolean = p.toString.endsWith(".spec")
        def isTag(p:  Path): Boolean = p.toString.endsWith(".tag")

        // 2 ──────────────────────────────────────────────────────────────
        // Load **all** HardwareSpecification objects.
        // - We **ignore** malformed files but log them verbosely for the user.
        // - Map by `id` for quick lookup.
        // ----------------------------------------------------------------
        val specs: Map[String, HardwareSpecification] =
          Files.walk(metaDir).iterator.asScala
            .filter(isSpec)
            .flatMap { path =>
              val txt = Files.readString(path)
              try   Some(uread[HardwareSpecification](txt))
              catch { case e: Throwable =>
                log.error(s"[spec-plugin] malformed .spec '${path.getFileName}': ${e.getMessage}")
                log.trace(e)
                None
              }
            }
            .map(s => s.id -> s)
            .toMap

        // 3 ──────────────────────────────────────────────────────────────
        // Load **all** Tag objects.
        // ----------------------------------------------------------------
        val tags: List[Tag] =
          Files.walk(metaDir).iterator.asScala
            .filter(isTag)
            .flatMap { path =>
              val txt = Files.readString(path)
              try   Some(uread[Tag](txt))
              catch { case e: Throwable =>
                log.error(s"[spec-plugin] malformed .tag '${path.getFileName}': ${e.getMessage}")
                log.trace(e)
                None
              }
            }
            .toList

        log.info(s"[spec-plugin] aggregated   specs = ${specs.size}   tags = ${tags.size}")

        // 4 ──────────────────────────────────────────────────────────────
        // Emit the *contractual* JSON indices expected by downstream tooling.
        // ----------------------------------------------------------------
        val outDir        = (Compile / target).value
        val specIndexFile = outDir / "SpecIndex.json"
        val modIndexFile  = outDir / "ModuleIndex.json"

        IO.write(specIndexFile, uwrite(specs.values.toList, indent = 2))
        IO.write(modIndexFile,  uwrite(tags,              indent = 2))

        log.success(s"SpecIndex  → ${specIndexFile.getAbsolutePath}")
        log.success(s"ModuleIndex→ ${modIndexFile.getAbsolutePath}")
    }

    /* --------------------------------------------------------------
     *  Make the test task depend on up‑to‑date JSON indices          
     * -------------------------------------------------------------- */
    (Test / test) := (Test / test).dependsOn(exportSpecIndex).value
    }
  )
}
