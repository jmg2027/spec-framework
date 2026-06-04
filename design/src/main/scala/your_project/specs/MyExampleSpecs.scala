package your_project.specs

import framework.macros.LocalSpec             // @LocalSpec macro
import framework.macros.SpecEmit.spec         // compile-time emission macro
import framework.spec.Spec._                  // DSL entry point (FUNCTION, PROPERTY …)
import framework.spec.HardwareSpecification

object MyExampleSpecs {
  // -----------------------------------------------------------------------------
  // HardwareSpecification test cases for CI/macro annotation coverage
  // -----------------------------------------------------------------------------
  // Test module spec for CI coverage
  val TestModuleSpec = spec {
    CONTRACT("TestModule").desc("Test module spec for CI coverage")
      .entry("ModuleKey", "ModuleValue")
      .build()
  }

  // Test interface spec for CI coverage
  val TestInterfaceSpec = spec {
    INTERFACE("TestInterface").desc("Test interface spec for CI coverage")
      .entry("InterfaceKey", "InterfaceValue")
      .build()
  }

  // 3. Test assign spec for CI coverage
  val TestAssignSpec = spec {
    FUNCTION("TestAssign").desc("Test assignment spec for CI coverage")
      .entry("AssignKey", "AssignValue")
      .build()
  }

  // Minimal FUNCTION spec (literal)
  val DummyLiteralSpec = spec {
    FUNCTION("DummyLiteral").desc("Dummy spec for literal test")
      .entry("Key", "Value")
      .build()
  }

  // PROPERTY spec with noCapability
  val DummyObjSpec = spec {
    PROPERTY("DummyObj").desc("Dummy spec for object test")
      .status("DRAFT")
      .entry("ObjKey", "ObjValue")
      .build()
  }

  // CONTRACT spec with parents/related
  val DummyCaseSpec = spec {
    CONTRACT("DummyCase").desc("Dummy spec for case class test")
      .entry("CaseKey", "CaseValue")
      .build()
  }

  // PARAMETER spec for expression/statement annotation
  val DummyWhenSpec = spec {
    FUNCTION("DummyWhen").desc("Dummy spec for when statement test")
      .entry("StatementKey", "StatementValue")
      .build()
  }

  val DummyAssignSpec = spec {
    FUNCTION("DummyAssign").desc("Dummy spec for assignment statement test")
      .entry("StatementKey", "StatementValue")
      .build()
  }

  val DummySwitchSpec = spec {
    FUNCTION("DummySwitch").desc("Dummy spec for switch statement test")
      .entry("StatementKey", "StatementValue")
      .build()
  }

  // INTERFACE spec with metadata and requiredCaps
  val DummyMetaSpec = spec {
    INTERFACE("DummyMeta").desc("Dummy spec with metadata and requiredCaps")
      .entry("MetaKey", "MetaValue")
      .build()
  }

  // RAW spec with custom prefix
  val DummyRawSpec = spec {
    RAW("DummyRaw", "RAW_PREFIX").desc("Dummy raw spec")
      .entry("RawKey", "RawValue")
      .build()
  }

  // FUNCTION spec with impl/verified
  val DummyImplSpec = spec {
    FUNCTION("DummyImpl").desc("Dummy spec with impl/verified")
      .entry("ImplKey", "ImplValue")
      .build()
  }

  // PROPERTY spec with multiple entries
  val DummyMultiEntrySpec = spec {
    PROPERTY("DummyMulti").desc("Dummy spec with multiple entries")
      .entry("A", "1").entry("B", "2").entry("C", "3")
      .build()
  }

  // FUNCTION spec with status and metadata
  val DummyStatusSpec = spec {
    FUNCTION("DummyStatus").desc("Dummy spec with status and metadata")
      .status("APPROVED")
      .entry("StatusKey", "StatusValue")
      .build()
  }

  // COVERAGE spec with related and parents
  val DummyCoverageSpec = spec {
    COVERAGE("DummyCoverage").desc("Dummy coverage spec")
      .entry("CovKey", "CovValue")
      .build()
  }

  // Comprehensive spec exercising all DSL builder methods
  val ComplexSpec = spec {
    CONTRACT("Complex").desc("Spec exercising all builder methods")
      .is("TestInterface", "DummyStatus")
      .has("DummyObj")
      .uses("TestModule")
      .status("DRAFT")
      .entry("Key1", "Val1").entry("Key2", "Val2")
      .entry("- Main Features")
      .entry("  - Performance")
      .entry("  - Reliability")
      .table("markdown", "|Signal|Width|\n|---|---|\n|clk|1|\n|data|32|")
      .draw("mermaid", "graph TD\nA --> B\nB --> C")
      .code("scala", """println("hi")""")
      .note("misc notes")
      .build()
  }

  // -----------------------------------------------------------------------------
  // End of HardwareSpecification test cases
  // -----------------------------------------------------------------------------
}
