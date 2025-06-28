// design/src/main/scala/your_project/design/Queue.scala
package your_project.design

import framework.macros.LocalSpec
import your_project.specs.MyExampleSpecs

/**
 * Example RTL/Chisel module tagged with a HardwareSpecification using @LocalSpec.
 * (This is a minimal stub for demonstration; replace with real Chisel/RTL code as needed.)
 */
@LocalSpec("QUEUE_FUNC_001")
class Queue {
  // ... RTL/Chisel implementation ...
  def enqueue(x: Int): Unit = ()
  def dequeue(): Int = 0
}

@LocalSpec("RESET_PROP_001")
object ResetLogic {
  def resetAll(): Unit = ()
}
