package framework.spec

import upickle.default.{ReadWriter, macroRW}

/** Entry used by exportSpecIndex task combining spec and tags. */
final case class SpecIndexEntry(
  id: String,
  spec: HardwareSpecification,
  tags: Seq[Tag]
)
object SpecIndexEntry {
  implicit val rw: ReadWriter[SpecIndexEntry] = macroRW
}
