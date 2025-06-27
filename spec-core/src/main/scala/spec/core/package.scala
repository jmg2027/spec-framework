package spec.core
import upickle.default.{ReadWriter, macroRW}

sealed trait SpecCategory { def prefix: String }
object SpecCategory {
  case object CONTRACT extends SpecCategory { val prefix = "CONTRACT" }
  case object FUNC     extends SpecCategory { val prefix = "FUNC"     }
  case object PROP     extends SpecCategory { val prefix = "PROP"     }
  case object COV      extends SpecCategory { val prefix = "COV"      }
  case object PARAM    extends SpecCategory { val prefix = "PARAM"    }

  final case class Raw(prefix: String) extends SpecCategory

//  implicit def rw: ReadWriter[SpecCategory] = macroRW[SpecCategory]
}

sealed trait Capability ; object Capability {
  case object BASIC_QUEUE  extends Capability { val name = "BASIC_QUEUE" }
  case object LOWPWR_QUEUE extends Capability { val name = "LOWPWR_QUEUE" }
  case object EXT_IP       extends Capability { val name = "EXT_IP"       }
  final case class Raw(name: String) extends Capability
  implicit val rw: ReadWriter[Capability] = macroRW
}

trait HardwareSpecification {
  def description: String
  def capability        : Capability  = null
  def parentSpecIds     : Set[String] = Set.empty
}
