package spec.core

sealed trait SpecCategory ; object SpecCategory {
  case object CONTRACT extends SpecCategory ; case object FUNC extends SpecCategory
  case object PROP extends SpecCategory     ; case object COV  extends SpecCategory
  final case class RAW(name: String) extends SpecCategory
}

sealed trait Capability ; object Capability {
  case object BASIC_QUEUE extends Capability
  final case class RAW(name: String) extends Capability
}

trait HardwareSpecification {
  def description: String
  def capability        : Capability  = null
  def parentSpecIds     : Set[String] = Set.empty
}
