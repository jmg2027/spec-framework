// spec-core/src/main/scala/framework/spec/MetaFile.scala
// Utility for compile-time emission of .spec and .tag files by macros/DSL.
// Used by the macro system to serialize and write metadata for aggregation.
package framework.spec

import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.util.UUID
import upickle.default // for upickle.write

/**
 * MetaFile: Utility object used at compile time by macros and the Spec DSL to
 * emit `.spec` and `.tag` files under the directory supplied via the system
 * property -Dspec.meta.dir=... (see build.sbt for setup).
 *
 * This enables compile-time generation of metadata for all hardware specs and
 * tags, which are later aggregated by the SBT plugin into JSON indices.
 */
object MetaFile {

  // ---------------------------------------------------------------------------
  // Resolve the target directory for metadata emission
  // ---------------------------------------------------------------------------
  // The directory is set via the system property 'spec.meta.dir', which should be
  // configured in build.sbt (see design/build.sbt for a reference setup).
  // Throws an error if not set, to ensure macro emission always has a target.
  private lazy val baseDir: Path = sys.props.get("spec.meta.dir") match {
    case Some(dirPath) => Paths.get(dirPath)
    case None          =>
      throw new IllegalStateException(
        "System property 'spec.meta.dir' not defined. " +
          "Configure it in build.sbt, e.g.:\n" +
          "  scalacOptions += s\"-Dspec.meta.dir=${(Compile / resourceManaged).value}/spec-meta\"",
      )
  }

  // ---------------------------------------------------------------------------
  // Public API: Called by macros/DSL to emit metadata files
  // ---------------------------------------------------------------------------

  /**
   * Serialize a `Tag` to JSON and write it as a .tag file in the spec-meta
   * directory. Called by the macro system when a tag is defined in user code.
   */
  def writeTag(tag: Tag): Unit = {
    writeFile(tag.id, "tag", upickle.default.write(tag, indent = 2))
    println(s"[MetaFile] wrote tag → spec-meta for id=${tag.id}")
  }

  /**
   * Serialize a `HardwareSpecification` to JSON and write it as a .spec file in
   * the spec-meta directory. Called by the macro system when a hardware spec is
   * defined in user code.
   */
  def writeSpec(spec: HardwareSpecification): Unit = {
    // Compose .spec file content: header lines (id, scalaDeclarationPath, ...) + JSON
    val header =
      s"id=${spec.id}\n" +
      (if (spec.scalaDeclarationPath.nonEmpty) s"scalaDeclarationPath=${spec.scalaDeclarationPath}\n" else "")
    val json = upickle.default.write(spec, indent = 2)
    writeFile(spec.id, "spec", header + json + "\n")
  }

  // ---------------------------------------------------------------------------
  // Internal helper: Write a JSON string to a file with a unique name
  // ---------------------------------------------------------------------------
  private def writeFile(id: String, ext: String, json: String): Unit =
    try {
      // Ensure the output directory exists
      Files.createDirectories(baseDir)

      // Compose a unique file name for each emission (id + random suffix)
      val fileName = s"${id}_${UUID.randomUUID().toString.take(8)}.$ext"
      val path     = baseDir.resolve(fileName)

      // Write the JSON string to the file, overwriting if it exists
      Files.writeString(
        path,
        json + "\n",
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE,
      )

      println(s"[MetaFile] wrote $ext → ${path.toAbsolutePath}")
    } catch {
      case e: Exception =>
        println(
          s"[MetaFile] failed to write $ext for id '$id': ${e.getMessage}",
        )
        throw e
    }
}
