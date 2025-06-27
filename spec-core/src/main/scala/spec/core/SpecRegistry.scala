package spec.core
import scala.collection.mutable
import upickle.default.{ReadWriter, macroRW}

object SpecRegistry {
  final case class Tag(
                        fqModule     : String,
                        instancePath : String,
                        category     : SpecCategory,
                        localId      : String,
                        capability   : Capability,
                        paramValues  : Map[String,String],
                        srcFile      : String,
                        line         : Int,
  )
  object Tag {
    implicit val tagRw: ReadWriter[Tag] = macroRW
  }

  private val buf = mutable.ListBuffer.empty[Tag]
  def add(t: Tag): Unit = buf += t
  def all: List[Tag]   = buf.toList
}
