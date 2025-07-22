// spec-core/src/main/scala/framework/spec/HardwareSpecification.scala
// Core data model: HardwareSpecification, SpecCategory, Capability
// This file defines the main types for representing hardware specifications,
// their categories, capabilities, and serialization logic for the framework.
package framework.spec

import upickle.default.{macroRW, readwriter, ReadWriter}

// ------------------------------------------------------------------------------
// HardwareSpecification: Main data structure for a hardware specification.
// All spec definitions are represented as instances of this class.
// This is a flat case class for simple upickle serialization.
// ------------------------------------------------------------------------------
final case class HardwareSpecification(
  id: String,                     // Unique identifier for the spec
  category: SpecCategory,         // Category/type of the spec (see below)
  description: String,            // Human-readable description
  status: Option[String] = None,  // Optional status (e.g., draft, verified)
  is: Set[String] = Set.empty,    // 'is' references (spec IDs)
  has: Set[String] = Set.empty,   // 'has' references (spec IDs)
  uses: Set[String] = Set.empty,  // 'uses' references (spec IDs)
  lists: List[(String, String)] = Nil, // List of key-value entries
  tables: List[String] = Nil,     // Markdown tables
  drawings: List[String] = Nil,   // Diagrams (markdown, SVG, etc.)
  codes: List[String] = Nil,      // Code snippets (markdown)
  notes: List[String] = Nil,      // Free-form notes
  scalaDeclarationPath: String = "",
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
    case object CAPABILITY         extends SpecCategory // Capability spec
    case object BUNDLE            extends SpecCategory // Bundle spec
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
        case SpecCategory.CAPABILITY  => "CAPABILITY"
        case SpecCategory.BUNDLE      => "BUNDLE"
        case SpecCategory.RAW(prefix) => s"RAW:$prefix"
      },
      {
        case "CONTRACT"                => SpecCategory.CONTRACT
        case "FUNCTION"                => SpecCategory.FUNCTION
        case "PROPERTY"                => SpecCategory.PROPERTY
        case "COVERAGE"                => SpecCategory.COVERAGE
        case "INTERFACE"               => SpecCategory.INTERFACE
        case "PARAMETER"               => SpecCategory.PARAMETER
        case "CAPABILITY"              => SpecCategory.CAPABILITY
        case "BUNDLE"                  => SpecCategory.BUNDLE
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
