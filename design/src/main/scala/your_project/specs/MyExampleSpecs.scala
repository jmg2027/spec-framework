package your_project.specs

import framework.macros.SpecEmit.emitSpec     // compile-time emission 매크로
import framework.spec.Spec._                  // DSL 진입점 (FUNCTION, PROPERTY …)
import framework.spec.Capability              // Capability 심볼
import framework.spec.HardwareSpecification

object MyExampleSpecs {

  /*───────────────── FUNCTION Spec ─────────────────*/
  val QueueSpec = emitSpec {
    FUNCTION("QUEUE_FUNC_001", "Queue must preserve FIFO")
      .capability(Capability("Queueing"))     // Stage1 → Stage2
      .status("DRAFT")
      .entry("Algorithm", "FIFO")
      .build()
  }

  // val QueueSpec: HardwareSpecification = new HardwareSpecification {
  //   id = "QUEUE_FUNC_001"
  //   description = "Queue must preserve FIFO"
  //   category = SpecCategory.FUNCTION
  //   capabilities += Capability("Queueing")
  //   status = "DRAFT"
  //   entries += ("Algorithm" -> "FIFO")
  // }
  /*───────────────── PROPERTY Spec ─────────────────*/
  val ResetSpec = emitSpec {
    PROPERTY("RESET_PROP_001", "Reset state to zero on reset signal.")
      .noCapability                           // Stage1 → Stage2
      .status("APPROVED")
      .entry("ResetType", "Synchronous")
      .entry("Value",     "Zero")
      .build()
  }
}
