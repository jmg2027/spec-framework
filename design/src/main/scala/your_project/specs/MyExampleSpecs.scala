// design/src/main/scala/your_project/specs/MyExampleSpecs.scala
package your_project.specs

import framework.spec._

/**
 * Example HardwareSpecification definitions using the builder DSL.
 */
object MyExampleSpecs {
  // Define a simple capability
  val QueueCap = Capability("Queueing")

  // Define a functional spec
  val QueueSpec: HardwareSpecification = new SpecBuilder(
    id = "QUEUE_FUNC_001",
    category = SpecCategory.FUNCTION,
    description = "Queue must preserve FIFO order for all enqueued elements."
  ).hasCapability(QueueCap)
   .withStatus("DRAFT")
   .withMetadata("author", "Alice")
   .apply()

  // Define a property spec
  val ResetSpec: HardwareSpecification = new SpecBuilder(
    id = "RESET_PROP_001",
    category = SpecCategory.PROPERTY,
    description = "Module must reset all internal state to zero on reset signal."
  ).withStatus("APPROVED")
   .withMetadata("author", "Bob")
   .apply()
}
