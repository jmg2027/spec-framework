// src/main/scala/your_project/specs/MyExampleSpecs.scala

package your_project.specs

import framework.spec._ // Spec, HardwareSpecification, SpecCategory, Capability 등이 포함됨
import Spec._ // Spec 객체의 팩토리 메서드를 직접 사용하기 위해 임포트

/**
 * Example HardwareSpecification definitions using the new staged builder DSL.
 */
object MyExampleSpecs {
  // Define a simple capability
  val QueueCap = Capability("Queueing")

  // Define a functional spec using the new Spec DSL
  val QueueSpec: HardwareSpecification = Spec.FUNCTION(
    id = "QUEUE_FUNC_001",
    desc = "Queue must preserve FIFO order for all enqueued elements."
  ).capability(QueueCap) // .hasCapability 대신 .capability
   .status("DRAFT")      // .withStatus 대신 .status
   .meta("author" -> "String.Alice", "priority" -> "high") // .withMetadata 대신 .meta, varargs 사용
   .entry("Purpose", "Ensures data integrity during queue operations.") // .withEntry 대신 .entry
   .entry("Algorithm", "FIFO")
   .build() // 최종적으로 build() 호출 (괄호 없음)

  // Define a property spec using the new Spec DSL
  val ResetSpec: HardwareSpecification = Spec.PROPERTY(
    id = "RESET_PROP_001",
    desc = "Module must reset all internal state to zero on reset signal."
  ).noCapability // capability가 없을 때 .noCapability 호출 (새로운 메서드)
   .status("APPROVED")
   .meta("author" -> "String.Bob")
   .entry("ResetType", "Synchronous")
   .entry("ResetValue", "Zero")
   .build()
}
