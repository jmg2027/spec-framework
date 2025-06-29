// spec-core/src/main/scala/framework/spec/HardwareSpecification.scala
// Core data model: HardwareSpecification, SpecEntry, SpecCategory, Capability
// This file defines the main types for representing hardware specifications,
// their categories, capabilities, and serialization logic for the framework.
package framework.spec

import upickle.default.{macroRW, readwriter, ReadWriter}

// ------------------------------------------------------------------------------
// SpecEntry: Represents a single key-value entry in a hardware specification.
// If you need a more complex hierarchy, use a sealed trait and upickle's ReadWriter.join.
// ------------------------------------------------------------------------------
final case class SpecEntry(name: String, value: String)
object SpecEntry {
  // upickle ReadWriter for SpecEntry (automatic via macro)
  implicit val rw: ReadWriter[SpecEntry] = macroRW
}

// ------------------------------------------------------------------------------
// HardwareSpecification: Main data structure for a hardware specification.
// All spec definitions are represented as instances of this class.
// This is a flat case class for simple upickle serialization.
// ------------------------------------------------------------------------------
final case class HardwareSpecification(
  id: String,                        // Unique identifier for the spec
  category: SpecCategory,            // Category/type of the spec (see below)
  description: String,               // Human-readable description
  capability: Option[Capability],    // Optional capability this spec provides
  status: Option[String],            // Optional status (e.g., draft, verified)
  metadata: Map[String, String],     // Arbitrary metadata key-value pairs
  parentIds: Set[String],            // Parent spec IDs (for hierarchy)
  relatedToIds: Set[String],         // Related spec IDs (cross-links)
  implementedBy: Option[
    String,
  ],                                 // Optional module/class that implements this spec
  verifiedBy: Option[String],        // Optional test/verification reference
  requiredCapabilities: Set[String], // Capabilities required by this spec
  definitionFile: Option[String],    // Source file where this spec is defined
  entries: List[SpecEntry],          // List of key-value entries for this spec
)
object HardwareSpecification {
  // upickle ReadWriter for HardwareSpecification (automatic via macro)
  implicit val rw: ReadWriter[HardwareSpecification] = macroRW
}

// ------------------------------------------------------------------------------
// SpecCategory: Enumerates the possible categories for a hardware spec.
// Uses a sealed trait for type safety and upickle custom serialization.
// ------------------------------------------------------------------------------
sealed trait SpecCategory
object SpecCategory {
  case object CONTRACT           extends SpecCategory // Contract-level spec
  case object FUNCTION           extends SpecCategory // Function-level spec
  case object PROPERTY           extends SpecCategory // Property-level spec
  case object COVERAGE           extends SpecCategory // Coverage spec
  case object INTERFACE          extends SpecCategory // Interface spec
  case object PARAMETER          extends SpecCategory // Parameter spec
  case class RAW(prefix: String) extends SpecCategory // Custom/raw category

  // upickle ReadWriter for SpecCategory (string-based encoding)
  implicit val rw: ReadWriter[SpecCategory] =
    readwriter[String].bimap[SpecCategory](
      {
        case SpecCategory.CONTRACT    => "CONTRACT"
        case SpecCategory.FUNCTION    => "FUNCTION"
        case SpecCategory.PROPERTY    => "PROPERTY"
        case SpecCategory.COVERAGE    => "COVERAGE"
        case SpecCategory.INTERFACE   => "INTERFACE"
        case SpecCategory.PARAMETER   => "PARAMETER"
        case SpecCategory.RAW(prefix) => s"RAW:$prefix"
      },
      {
        case "CONTRACT"                => SpecCategory.CONTRACT
        case "FUNCTION"                => SpecCategory.FUNCTION
        case "PROPERTY"                => SpecCategory.PROPERTY
        case "COVERAGE"                => SpecCategory.COVERAGE
        case "INTERFACE"               => SpecCategory.INTERFACE
        case "PARAMETER"               => SpecCategory.PARAMETER
        case s if s.startsWith("RAW:") => SpecCategory.RAW(s.drop(4))
        case other                     =>
          throw new IllegalArgumentException(s"Unknown SpecCategory: $other")
      },
    )
}

// ------------------------------------------------------------------------------
// Capability: Represents a named capability that a spec can provide or require.
// ------------------------------------------------------------------------------
final case class Capability(name: String)
object Capability {
  // upickle ReadWriter for Capability (automatic via macro)
  implicit val rw: ReadWriter[Capability] = macroRW
}
