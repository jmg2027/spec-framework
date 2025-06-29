
// =============================================================================
// spec-plugin/src/main/scala/framework/plugin/SpecPlugin.scala
//
// SBT AutoPlugin for aggregating hardware spec and tag metadata.
// - Triggers macro side-effects by loading all classes (for @LocalSpec, etc.)
// - Collects emitted .spec/.tag files from macro runs
// - Aggregates and emits SpecIndex.json and ModuleIndex.json for downstream use
//
// All comments follow the project documentation style guide: concise, English, and practical.
// =============================================================================

package framework.plugin


import sbt._, Keys._
// Import core types to ensure macro side-effects are triggered during classloading
import _root_.framework.spec.{HardwareSpecification, SpecRegistry, Tag}

import java.net.URLClassLoader
import java.nio.file.{Files, Path}
import scala.collection.JavaConverters._

object SpecPlugin extends AutoPlugin {
  // ========================= Auto Import =========================
  // All auto-imported tasks and settings for build.sbt usage
  object autoImport {
    /**
     * SBT task: exportSpecIndex
     * Triggers compilation, loads all classes to fire macro side-effects,
     * and aggregates emitted .spec/.tag files into JSON indices for downstream use.
     */
    val exportSpecIndex = taskKey[Unit]("Emit Spec & Tag indices")
  }
  import autoImport._

  // ========================= Plugin Requirements =========================
  // This plugin requires the standard JVM plugin
  override def requires = plugins.JvmPlugin

  // ========================= Main Project Settings =========================
  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    // Main task: exportSpecIndex
    exportSpecIndex := {
      // Step 0: Compile the project to ensure all classes are up-to-date
      val analysis = (Compile / compile).value
      val log      = streams.value.log

      // Step 1: Eagerly load all classes in the output directory to trigger
      // macro side-effects (e.g., @LocalSpec) that emit .spec/.tag files at compile time
      val dir  = (Compile / classDirectory).value
      val urls = Array(dir.toURI.toURL)
      val cl   = new URLClassLoader(urls, this.getClass.getClassLoader)

      // Recursively walk all .class files and try to load them
      Files.walk(dir.toPath).iterator.asScala
        .filter(p => p.toString.endsWith(".class"))
        .foreach { p =>
          // Convert file path to fully qualified class name
          val fqcn = dir.toPath.relativize(p).toString
            .stripSuffix(".class").replace('/', '.')
          // Try to load the class; ignore failures (e.g., Chisel modules, anonymous classes)
          try Class.forName(fqcn, /*initialize =*/ true, cl)
          catch { case _: Throwable => () }
        }

      // Step 2: Aggregate emitted .spec and .tag files from the spec-meta directory
      val metaDir = (Compile / resourceManaged).value.toPath.resolve("spec-meta")
      if (!Files.exists(metaDir)) {
        // If no metadata was generated, emit empty JSON index files
        log.warn(s"No spec metadata generated; '${metaDir.toAbsolutePath}' is absent.")
        writeEmptyJsons()
      } else {
        // Aggregate all .spec and .tag files into JSON index files
        aggregate(metaDir)
      }

      // ----------------------------------------------------------------------
      // Helper: Write empty JSON index files if no metadata is present
      def writeEmptyJsons(): Unit = {
        val outDir = (Compile / target).value.toPath
        Files.createDirectories(outDir)
        Files.writeString(outDir.resolve("SpecIndex.json"),  "[]\n")
        Files.writeString(outDir.resolve("ModuleIndex.json"), "[]\n")
        log.info(s"SpecIndex generated  → ${outDir.resolve("SpecIndex.json")}")
        log.info(s"ModuleIndex generated → ${outDir.resolve("ModuleIndex.json")}")
      }

      // ----------------------------------------------------------------------
      // Helper: Aggregate all .spec and .tag files into JSON index files
      def aggregate(meta: Path): Unit = {
        // Collect all .spec files and parse as JSON
        val specs = Files.walk(meta).iterator.asScala
          .filter(_.toString.endsWith(".spec"))
          .map(p => ujson.read(Files.readString(p))).toVector
        // Collect all .tag files and parse as JSON
        val tags  = Files.walk(meta).iterator.asScala
          .filter(_.toString.endsWith(".tag"))
          .map(p => ujson.read(Files.readString(p))).toVector

        val outDir = (Compile / target).value.toPath
        Files.createDirectories(outDir)
        // Write aggregated JSON arrays to output files
        Files.writeString(outDir.resolve("SpecIndex.json"),  ujson.write(specs,  indent = 2))
        Files.writeString(outDir.resolve("ModuleIndex.json"), ujson.write(tags, indent = 2))
        log.info(s"SpecIndex generated  → ${outDir.resolve("SpecIndex.json")}")
        log.info(s"ModuleIndex generated → ${outDir.resolve("ModuleIndex.json")}")
        log.info(s"Aggregated ${specs.size} specs, ${tags.size} tags")
      }
    },

    // Ensure tests always run after the spec index is exported, keeping test resources up-to-date
    (Test / test) := (Test / test).dependsOn(exportSpecIndex).value
  )
}
