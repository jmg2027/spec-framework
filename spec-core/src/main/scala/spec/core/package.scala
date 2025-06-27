package spec.core

sealed trait TagCat { def prefix: String }
object TagCat {
  case object CONTRACT extends TagCat { val prefix = "CONTRACT" }
  case object FUNC     extends TagCat { val prefix = "FUNC"     }
  case object PROP     extends TagCat { val prefix = "PROP"     }
  case object COV      extends TagCat { val prefix = "COV"      }
  case object PARAM    extends TagCat { val prefix = "PARAM"    }
  final case class Raw(prefix: String) extends TagCat
}

sealed trait Capability ; object Capability {
  case object BASIC_QUEUE  extends Capability { val name = "BASIC_QUEUE" }
  case object LOWPWR_QUEUE extends Capability { val name = "LOWPWR_QUEUE" }
  case object EXT_IP       extends Capability { val name = "EXT_IP"       }
  final case class Raw(name: String) extends Capability
}

trait HardwareSpecification {
  def description: String
  def capability        : Capability  = null
  def parentSpecIds     : Set[String] = Set.empty
}
