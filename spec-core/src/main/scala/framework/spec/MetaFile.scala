package framework.spec

import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.util.UUID
import upickle.default                                    // for upickle.write

/**
 * Utility object used at *compile time* by macros and the Spec DSL
 * to emit `.spec` and `.tag` files under the directory supplied via
 * the system property  -Dspec.meta.dir=...
 */
object MetaFile {

  // ---------------------------------------------------------------------------
  // Resolve target directory
  // ---------------------------------------------------------------------------
  private lazy val baseDir: Path = sys.props.get("spec.meta.dir") match {
    case Some(dirPath) => Paths.get(dirPath)
    case None =>
      throw new IllegalStateException(
        "System property 'spec.meta.dir' not defined. " +
        "Configure it in build.sbt, e.g.:\n" +
        """  scalacOptions += s"-Dspec.meta.dir=${(Compile / resourceManaged).value}/spec-meta" """
      )
  }

  // ---------------------------------------------------------------------------
  // Public API
  // ---------------------------------------------------------------------------
  /** Serialize a `Tag` to  JSON  and write it as  *.tag*  */
  def writeTag(tag: Tag): Unit = {
    writeFile(tag.id, "tag", upickle.default.write(tag, indent = 2))
    println(s"[MetaFile] wrote tag → spec-meta for id=${tag.id}")
  }

  /** Serialize a `HardwareSpecification` to JSON and write it as *.spec* */
  def writeSpec(spec: HardwareSpecification): Unit =
    writeFile(spec.id, "spec", upickle.default.write(spec, indent = 2))

  // ---------------------------------------------------------------------------
  // Internal helpers
  // ---------------------------------------------------------------------------
  private def writeFile(id: String, ext: String, json: String): Unit = {
    try {
      Files.createDirectories(baseDir)

      val fileName = s"${id}_${UUID.randomUUID().toString.take(8)}.$ext"
      val path     = baseDir.resolve(fileName)

      Files.writeString(
        path,
        json + "\n",
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
      )

      println(s"[MetaFile] wrote $ext → ${path.toAbsolutePath}")
    } catch {
      case e: Exception =>
        println(s"[MetaFile] failed to write $ext for id '$id': ${e.getMessage}")
        throw e
    }
  }
}
