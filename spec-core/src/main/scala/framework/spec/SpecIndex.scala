// spec-core/src/main/scala/framework/spec/SpecIndex.scala
// Utility for mapping scalaDeclarationPath to specId by scanning .spec files.
package framework.spec

import java.io.File
import scala.io.Source

object SpecIndex {
  // Directory containing .spec files (set by -Dspec.meta.dir)
  private lazy val baseDir: File =
    sys.props.get("spec.meta.dir").map(new File(_)).getOrElse(
      throw new IllegalStateException(
        "System property 'spec.meta.dir' not defined. Set it via sbt/javaOptions."
      )
    )

  // Map: scalaDeclarationPath -> specId
  lazy val map: Map[String, String] = {
    if (!baseDir.exists || !baseDir.isDirectory) return Map.empty
    val specFiles = baseDir.listFiles().filter(f => f.getName.endsWith(".spec"))
    specFiles.flatMap { file =>
      val lines = Source.fromFile(file).getLines().toList
      val idOpt = lines.find(_.startsWith("id=")).map(_.drop(3).trim)
      val pathOpt = lines.find(_.startsWith("scalaDeclarationPath=")).map(_.drop(21).trim)
      for (id <- idOpt; path <- pathOpt) yield path -> id
    }.toMap
  }

  // For macro: lookup by symbol fullName
  def idFor(fullName: String): Option[String] = map.get(fullName)
}
