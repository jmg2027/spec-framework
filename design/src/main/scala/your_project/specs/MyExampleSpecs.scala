package your_project.specs

import framework.macros.LocalSpec             // @LocalSpec 매크로
import framework.macros.SpecEmit.spec         // compile-time emission 매크로
import framework.spec.Spec._                  // DSL 진입점 (FUNCTION, PROPERTY …)
import framework.spec.Capability              // Capability 심볼
import framework.spec.HardwareSpecification

object MyExampleSpecs {
  // -----------------------------------------------------------------------------
  // HardwareSpecification test cases for CI/macro annotation coverage
  // -----------------------------------------------------------------------------
  // Test module spec for CI coverage
  val TestModuleSpec = spec {
    CONTRACT("TEST_MODULE").desc("Test module spec for CI coverage")
      .entry("ModuleKey", "ModuleValue")
      .build()
  }

  // Test interface spec for CI coverage
  val TestInterfaceSpec = spec {
    INTERFACE("TEST_INTERFACE").desc("Test interface spec for CI coverage")
      .entry("InterfaceKey", "InterfaceValue")
      .build()
  }

  // 3. Test assign spec for CI coverage
  val TestAssignSpec = spec {
    FUNCTION("TEST_ASSIGN").desc("Test assignment spec for CI coverage")
      .entry("AssignKey", "AssignValue")
      .build()
  }

  // Minimal FUNCTION spec (literal)
  val DummyLiteralSpec = spec {
    FUNCTION("DUMMY_LITERAL").desc("Dummy spec for literal test")
      .entry("Key", "Value")
      .build()
  }

  // PROPERTY spec with noCapability
  val DummyObjSpec = spec {
    PROPERTY("DUMMY_OBJ").desc("Dummy spec for object test")
      .status("DRAFT")
      .entry("ObjKey", "ObjValue")
      .build()
  }

  // CONTRACT spec with parents/related
  val DummyCaseSpec = spec {
    CONTRACT("DUMMY_CASE").desc("Dummy spec for case class test")
      .entry("CaseKey", "CaseValue")
      .build()
  }

  // PARAMETER spec for expression/statement annotation
  val DummyWhenSpec = spec {
    FUNCTION("DUMMY_WHEN").desc("Dummy spec for when statement test")
      .entry("StatementKey", "StatementValue")
      .build()
  }

  val DummyAssignSpec = spec {
    FUNCTION("DUMMY_ASSIGNN").desc("Dummy spec for assignment statement test")
      .entry("StatementKey", "StatementValue")
      .build()
  }

  val DummySwitchSpec = spec {
    FUNCTION("DUMMY_Switch").desc("Dummy spec for switch statement test")
      .entry("StatementKey", "StatementValue")
      .build()
  }

  // INTERFACE spec with metadata and requiredCaps
  val DummyMetaSpec = spec {
    INTERFACE("DUMMY_META").desc("Dummy spec with metadata and requiredCaps")
      .entry("MetaKey", "MetaValue")
      .build()
  }

  // RAW spec with custom prefix
  val DummyRawSpec = spec {
    RAW("DUMMY_RAW", "RAW_PREFIX").desc("Dummy raw spec")
      .entry("RawKey", "RawValue")
      .build()
  }

  // FUNCTION spec with impl/verified
  val DummyImplSpec = spec {
    FUNCTION("DUMMY_IMPL").desc("Dummy spec with impl/verified")
      .entry("ImplKey", "ImplValue")
      .build()
  }

  // PROPERTY spec with multiple entries
  val DummyMultiEntrySpec = spec {
    PROPERTY("DUMMY_MULTI").desc("Dummy spec with multiple entries")
      .entry("A", "1").entry("B", "2").entry("C", "3")
      .build()
  }

  // FUNCTION spec with status and metadata
  val DummyStatusSpec = spec {
    FUNCTION("DUMMY_STATUS").desc("Dummy spec with status and metadata")
      .status("APPROVED")
      .entry("StatusKey", "StatusValue")
      .build()
  }

  // COVERAGE spec with related and parents
  val DummyCoverageSpec = spec {
    COVERAGE("DUMMY_COVERAGE").desc("Dummy coverage spec")
      .entry("CovKey", "CovValue")
      .build()
  }

  // Comprehensive spec exercising all DSL builder methods
  val ComplexSpec = spec {
    CONTRACT("COMPLEX").desc("Spec exercising all builder methods")
      .is("TEST_INTERFACE", "DUMMY_STATUS")
      .has("DUMMY_OBJ")
      .uses("TEST_MODULE")
      .status("DRAFT")
      .entry("Key1", "Val1").entry("Key2", "Val2")
      .table("|A|B|\n|1|2|")
      .draw("diagram.svg")
      .code("""```scala
        println("hi")
      ```""")
      .note("misc notes")
      .build()
  }

  // -----------------------------------------------------------------------------
  // End of HardwareSpecification test cases
  // -----------------------------------------------------------------------------
}
