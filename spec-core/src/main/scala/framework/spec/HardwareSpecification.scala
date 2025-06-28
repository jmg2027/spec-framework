// spec-core/src/main/scala/framework/spec/HardwareSpecification.scala

package framework.spec

import upickle.default.{ReadWriter, macroRW, readwriter}

// (NEW): SpecEntry 정의 (HardwareSpecification에 포함될 필드)
// 이 예시에서는 SpecEntry가 간단한 case class라고 가정합니다.
// 만약 SpecEntry도 복잡한 계층 구조를 가진다면, SpecCategory처럼 sealed trait와 ReadWriter.join이 필요합니다.
final case class SpecEntry(name: String, value: String)
object SpecEntry {
  implicit val rw: ReadWriter[SpecEntry] = macroRW
}

/**
 * HardwareSpecification: 하드웨어 스펙의 구체적인 정의를 나타내는 final case class입니다.
 * 모든 스펙 정의는 이 클래스의 인스턴스로 표현됩니다.
 * (수정됨): sealed trait + inner case class 대신 단일 final case class로 변경하여 upickle 직렬화를 단순화합니다.
 */
final case class HardwareSpecification(
  id: String,
  category: SpecCategory,
  description: String,
  capability: Option[Capability],
  status: Option[String],
  metadata: Map[String, String],
  parentIds: Set[String],
  relatedToIds: Set[String],
  implementedBy: Option[String],
  verifiedBy: Option[String],
  requiredCapabilities: Set[String],
  definitionFile: Option[String],
  entries: List[SpecEntry]
)
object HardwareSpecification {
  // (NEW): 단일 case class에 대한 upickle ReadWriter 정의는 macroRW 하나로 충분합니다.
  implicit val rw: ReadWriter[HardwareSpecification] = macroRW
}

// SpecCategory 정의 (sealed trait 및 object)
// (이전과 동일하게 유지 - upickle ReadWriter도 이미 잘 정의되어 있음)
sealed trait SpecCategory
object SpecCategory {
  case object CONTRACT extends SpecCategory
  case object FUNCTION extends SpecCategory
  case object PROPERTY extends SpecCategory
  case object COVERAGE extends SpecCategory
  case object INTERFACE extends SpecCategory
  case object PARAMETER extends SpecCategory
  case class RAW(prefix: String) extends SpecCategory


implicit val rw: ReadWriter[SpecCategory] =
  readwriter[String].bimap[SpecCategory](
    {
      case SpecCategory.CONTRACT        => "CONTRACT"
      case SpecCategory.FUNCTION        => "FUNCTION"
      case SpecCategory.PROPERTY        => "PROPERTY"
      case SpecCategory.COVERAGE        => "COVERAGE"
      case SpecCategory.INTERFACE       => "INTERFACE"
      case SpecCategory.PARAMETER       => "PARAMETER"
      case SpecCategory.RAW(prefix)     => s"RAW:$prefix"
    },
    {
      case "CONTRACT"                   => SpecCategory.CONTRACT
      case "FUNCTION"                   => SpecCategory.FUNCTION
      case "PROPERTY"                   => SpecCategory.PROPERTY
      case "COVERAGE"                   => SpecCategory.COVERAGE
      case "INTERFACE"                  => SpecCategory.INTERFACE
      case "PARAMETER"                  => SpecCategory.PARAMETER
      case s if s.startsWith("RAW:")    => SpecCategory.RAW(s.drop(4))
      case other                        => throw new IllegalArgumentException(s"Unknown SpecCategory: $other")
    }
  )
}

// Capability 정의 (이전과 동일하게 유지)
final case class Capability(name: String)
object Capability {
  implicit val rw: ReadWriter[Capability] = macroRW
}
