// spec-core/src/main/scala/framework/spec/Tag.scala
package framework.spec

import upickle.default.{ReadWriter, macroRW}

/**
 * Tag (LocalSpec annotation metadata)
 */
final case class Tag(
  id: String,
  fullyQualifiedModuleName: String,
  hardwareInstancePath: String,
  srcFile: String,
  line: Int,
  column: Int
)
object Tag { implicit val rw: ReadWriter[Tag] = macroRW }
