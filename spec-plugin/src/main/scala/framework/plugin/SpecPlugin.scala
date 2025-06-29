// spec-plugin/src/main/scala/spec/plugin/SpecPlugin.scala  (full file below)
package framework.plugin

import sbt._, Keys._
import _root_.framework.spec.{HardwareSpecification, SpecRegistry, Tag} // For annotating codegen macros

import java.net.URLClassLoader
import java.nio.file.{Files, Path}
import scala.collection.JavaConverters._

object SpecPlugin extends AutoPlugin {
  object autoImport { val exportSpecIndex = taskKey[Unit]("Emit Spec & Tag indices") }
  import autoImport._

  override def requires = plugins.JvmPlugin

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    exportSpecIndex := {
      /* 0 ─ compile first */
      val analysis = (Compile / compile).value
      val log      = streams.value.log

      /* 1 ─ Eager-load every class to fire the @LocalSpec side-effects */
      val dir  = (Compile / classDirectory).value
      val urls = Array(dir.toURI.toURL)
      val cl   = new URLClassLoader(urls, this.getClass.getClassLoader)

      Files.walk(dir.toPath).iterator.asScala
        .filter(p => p.toString.endsWith(".class"))
        .foreach { p =>
          val fqcn = dir.toPath.relativize(p).toString
            .stripSuffix(".class").replace('/', '.')
          // Try-load; ignore failures from Chisel Module classes etc.
          try Class.forName(fqcn, /*initialize =*/ true, cl)
          catch { case _: Throwable => () }
        }

      /* 2 ─ now the spec-meta directory should exist (if any tags present) */
      val metaDir = (Compile / resourceManaged).value.toPath.resolve("spec-meta")
      if (!Files.exists(metaDir)) {
        log.warn(s"No spec metadata generated; '${metaDir.toAbsolutePath}' is absent.")
        writeEmptyJsons()
      } else {
        aggregate(metaDir)
      }

      // ─ helpers ───────────────────────────────────────────────
      def writeEmptyJsons(): Unit = {
        val outDir = (Compile / target).value.toPath; Files.createDirectories(outDir)
        Files.writeString(outDir.resolve("SpecIndex.json"),  "[]\n")
        Files.writeString(outDir.resolve("ModuleIndex.json"), "[]\n")
        log.info(s"SpecIndex generated  → ${outDir.resolve("SpecIndex.json")}")
        log.info(s"ModuleIndex generated → ${outDir.resolve("ModuleIndex.json")}")
      }

      def aggregate(meta: Path): Unit = {
        val specs = Files.walk(meta).iterator.asScala
          .filter(_.toString.endsWith(".spec"))
          .map(p => ujson.read(Files.readString(p))).toVector
        val tags  = Files.walk(meta).iterator.asScala
          .filter(_.toString.endsWith(".tag"))
          .map(p => ujson.read(Files.readString(p))).toVector

        val outDir = (Compile / target).value.toPath; Files.createDirectories(outDir)
        Files.writeString(outDir.resolve("SpecIndex.json"),  ujson.write(specs,  indent = 2))
        Files.writeString(outDir.resolve("ModuleIndex.json"), ujson.write(tags, indent = 2))
        log.info(s"SpecIndex generated  → ${outDir.resolve("SpecIndex.json")}")
        log.info(s"ModuleIndex generated → ${outDir.resolve("ModuleIndex.json")}")
        log.info(s"Aggregated ${specs.size} specs, ${tags.size} tags")
      }
    },

    /* keep tests up-to-date */
    (Test / test) := (Test / test).dependsOn(exportSpecIndex).value
  )
}
