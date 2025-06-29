// spec-core/src/main/scala/framework/spec/Tag.scala
// Tag metadata type for hardware spec annotation (@LocalSpec)
// Used for compile-time emission and plugin aggregation.
package framework.spec

import upickle.default.{macroRW, ReadWriter}

/**
 * Tag: Metadata emitted by the @LocalSpec macro annotation.
 *
 * Each Tag instance represents a single hardware module or instance annotated
 * in user code. This metadata is serialized to a .tag file at compile time and
 * later aggregated by the SBT plugin.
 *
 * @param id
 *   Unique tag identifier (usually user-defined or macro-generated)
 * @param fullyQualifiedModuleName
 *   Full Scala/Chisel module name (with package)
 * @param hardwareInstancePath
 *   Hierarchical hardware instance path (for Chisel/RTL)
 * @param srcFile
 *   Source file where the annotation appears
 * @param line
 *   Line number in the source file
 * @param column
 *   Column number in the source file
 */
final case class Tag(
  id: String,
  fullyQualifiedModuleName: String,
  hardwareInstancePath: String,
  srcFile: String,
  line: Int,
  column: Int,
)

object Tag {
  // upickle ReadWriter for Tag (automatic via macro)
  implicit val rw: ReadWriter[Tag] = macroRW
}
