// spec-core/src/main/scala/framework/spec/HardwareSpecification.scala
package framework.spec

import upickle.default.{ReadWriter, macroRW}

// SpecCategory definition
sealed trait SpecCategory
object SpecCategory {
  case object CONTRACT extends SpecCategory
  case object FUNCTION extends SpecCategory
  case object PROPERTY extends SpecCategory
  case object COVERAGE extends SpecCategory
  case object INTERFACE extends SpecCategory
  case object PARAMETER extends SpecCategory
  case class RAW(prefix: String) extends SpecCategory

  import upickle.default.readwriter
  implicit val rw: ReadWriter[SpecCategory] = readwriter[String].bimap[SpecCategory](
    {
      case CONTRACT   => "CONTRACT"
      case FUNCTION   => "FUNCTION"
      case PROPERTY   => "PROPERTY"
      case COVERAGE   => "COVERAGE"
      case INTERFACE  => "INTERFACE"
      case PARAMETER  => "PARAMETER"
      case RAW(p)     => s"RAW:$p"
    },
    {
      case "CONTRACT"   => CONTRACT
      case "FUNCTION"   => FUNCTION
      case "PROPERTY"   => PROPERTY
      case "COVERAGE"   => COVERAGE
      case "INTERFACE"  => INTERFACE
      case "PARAMETER"  => PARAMETER
      case s if s.startsWith("RAW:") => RAW(s.stripPrefix("RAW:"))
      case other => throw new Exception(s"Unknown SpecCategory: $other")
    }
  )
}

// Capability definition
case class Capability(name: String)
object Capability { implicit val rw: ReadWriter[Capability] = macroRW }

/**
 * Base definition for hardware specifications.
 */
abstract class HardwareSpecification {
  val id: String
  val category: SpecCategory
  val description: String
  val capability: Option[Capability] = None
  val parentIds: Set[String] = Set.empty
  val metadata: Map[String, String] = Map.empty
  val status: Option[String] = None
  val relatedToIds: Set[String] = Set.empty
  val implementedBy: Option[String] = None
  val verifiedBy: Option[String] = None
  val requiredCapabilities: Set[String] = Set.empty
  val definitionFile: Option[String] = None
}
