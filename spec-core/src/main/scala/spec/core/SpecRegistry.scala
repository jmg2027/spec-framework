package spec.core
import scala.collection.mutable

object SpecRegistry {
  final case class Tag(
    fqModule     : String,
    instancePath : String,
    category     : TagCat,
    localId      : String,
    capability   : Capability,
    paramValues  : Map[String,String],
    srcFile      : String,
    line         : Int
  )
  private val buf = mutable.ListBuffer.empty[Tag]
  def add(t: Tag): Unit = buf += t
  def all: List[Tag]   = buf.toList
}
