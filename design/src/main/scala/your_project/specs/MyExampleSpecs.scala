package your_project.specs

import framework.macros.LocalSpec             // @LocalSpec 매크로
import framework.macros.SpecEmit.emitSpec     // compile-time emission 매크로
import framework.spec.Spec._                  // DSL 진입점 (FUNCTION, PROPERTY …)
import framework.spec.Capability              // Capability 심볼
import framework.spec.HardwareSpecification

object MyExampleSpecs {
  // -----------------------------------------------------------------------------
  // HardwareSpecification test cases for CI/macro annotation coverage
  // -----------------------------------------------------------------------------
  // Test module spec for CI coverage
  val TestModuleSpec = emitSpec {
    CONTRACT("TEST_MODULE", "Test module spec for CI coverage")
      .capability(Capability("Test"))
      .entry("ModuleKey", "ModuleValue")
      .build()
  }

  // Test interface spec for CI coverage
  val TestInterfaceSpec = emitSpec {
    INTERFACE("TEST_INTERFACE", "Test interface spec for CI coverage")
      .capability(Capability("Test"))
      .entry("InterfaceKey", "InterfaceValue")
      .build()
  }

  // 3. Test assign spec for CI coverage
  val TestAssignSpec = emitSpec {
    FUNCTION("TEST_ASSIGN", "Test assignment spec for CI coverage")
      .noCapability
      .entry("AssignKey", "AssignValue")
      .build()
  }

  // Minimal FUNCTION spec (literal)
  val DummyLiteralSpec = emitSpec {
    FUNCTION("DUMMY_LITERAL", "Dummy spec for literal test")
      .capability(Capability("Test"))
      .entry("Key", "Value")
      .build()
  }

  // PROPERTY spec with noCapability
  val DummyObjSpec = emitSpec {
    PROPERTY("DUMMY_OBJ", "Dummy spec for object test")
      .noCapability
      .status("DRAFT")
      .entry("ObjKey", "ObjValue")
      .build()
  }

  // CONTRACT spec with parents/related
  val DummyCaseSpec = emitSpec {
    CONTRACT("DUMMY_CASE", "Dummy spec for case class test")
      .capability(Capability("Contractual"))
      .parents("BASE1", "BASE2")
      .related("REL1")
      .entry("CaseKey", "CaseValue")
      .build()
  }

  // PARAMETER spec for expression/statement annotation
  val DummyWhenSpec = emitSpec {
    FUNCTION("DUMMY_WHEN", "Dummy spec for when statement test")
      .noCapability
      .entry("StatementKey", "StatementValue")
      .build()
  }

  val DummyAssignSpec = emitSpec {
    FUNCTION("DUMMY_ASSIGNN", "Dummy spec for assignment statement test")
      .noCapability
      .entry("StatementKey", "StatementValue")
      .build()
  }

  val DummySwitchSpec = emitSpec {
    FUNCTION("DUMMY_Switch", "Dummy spec for switch statement test")
      .noCapability
      .entry("StatementKey", "StatementValue")
      .build()
  }

  // INTERFACE spec with metadata and requiredCaps
  val DummyMetaSpec = emitSpec {
    INTERFACE("DUMMY_META", "Dummy spec with metadata and requiredCaps")
      .capability(Capability("Meta"))
      .meta("foo" -> "bar", "baz" -> "qux")
      .requiresCaps("CAP1", "CAP2")
      .entry("MetaKey", "MetaValue")
      .build()
  }

  // RAW spec with custom prefix
  val DummyRawSpec = emitSpec {
    RAW("DUMMY_RAW", "Dummy raw spec", "RAW_PREFIX")
      .noCapability
      .entry("RawKey", "RawValue")
      .build()
  }

  // FUNCTION spec with impl/verified
  val DummyImplSpec = emitSpec {
    FUNCTION("DUMMY_IMPL", "Dummy spec with impl/verified")
      .capability(Capability("Impl"))
      .impl("SomeModule")
      .verified("SomeTest")
      .entry("ImplKey", "ImplValue")
      .build()
  }

  // PROPERTY spec with multiple entries
  val DummyMultiEntrySpec = emitSpec {
    PROPERTY("DUMMY_MULTI", "Dummy spec with multiple entries")
      .noCapability
      .entry("A", "1").entry("B", "2").entry("C", "3")
      .build()
  }

  // FUNCTION spec with status and metadata
  val DummyStatusSpec = emitSpec {
    FUNCTION("DUMMY_STATUS", "Dummy spec with status and metadata")
      .capability(Capability("Status"))
      .status("APPROVED")
      .meta("approvedBy" -> "CI")
      .entry("StatusKey", "StatusValue")
      .build()
  }

  // COVERAGE spec with related and parents
  val DummyCoverageSpec = emitSpec {
    COVERAGE("DUMMY_COVERAGE", "Dummy coverage spec")
      .capability(Capability("Coverage"))
      .related("REL_COV")
      .parents("BASE_COV")
      .entry("CovKey", "CovValue")
      .build()
  }

  // -----------------------------------------------------------------------------
  // End of HardwareSpecification test cases
  // -----------------------------------------------------------------------------
}
