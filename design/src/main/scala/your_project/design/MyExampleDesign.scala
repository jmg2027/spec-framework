// design/src/main/scala/your_project/design/Queue.scala
package your_project.design

import chisel3._
import chisel3.util._
import framework.macros.LocalSpec
import your_project.specs.MyExampleSpecs._

// -----------------------------------------------------------------------------
// CI Macro Annotation Test Cases for @LocalSpec
// Each case below is designed to test a different usage pattern or Chisel/Scala construct.
// -----------------------------------------------------------------------------
// -----------------------------------------------------------------------------
// Example: Tagging expressions/statements that cannot be directly annotated
// (e.g., when, :=, if, etc.)
// These are placed inside a test module for CI/macro annotation coverage.
// -----------------------------------------------------------------------------
@LocalSpec(TestModuleSpec)
class TestModule extends Module {

  @LocalSpec(TestInterfaceSpec)
  val io = IO(new Bundle {
    val in = Input(UInt(8.W))
    val out = Output(UInt(8.W))
  })

  @LocalSpec(TestAssignSpec)
  val a = Wire(UInt(8.W))

  // Chisel 'when' cannot be annotated directly, so tag a dummy val just above
  @LocalSpec(DummyWhenSpec)
  val dummyspecWhen = () // Tag for the following when-case
  when(io.in === 0.U) {
    io.out := 0.U
  }

  // Chisel ':=' cannot be annotated directly, so tag a dummy val just above
  @LocalSpec(DummyAssignSpec)
  val dummyspecAssign = () // Tag for the following assignment
  a := 42.U

  // Chisel 'if' cannot be annotated directly, so tag a dummy val just above
  @LocalSpec(DummySwitchSpec)
  val dummyspecSwitch = () // Tag for the following if-statement
  switch(a) {
    is(42.U) {
      io.out := 1.U
    }
    is(0.U) {
      io.out := 0.U
    }
    // Default case removed for compilation
  }
}

// -----------------------------------------------------------------------------
// End of dummy spec and expression annotation test cases
// -----------------------------------------------------------------------------
