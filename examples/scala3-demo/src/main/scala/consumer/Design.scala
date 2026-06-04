// the RTL, anchored to the specs via localSpec / assertProperty (Scala 3)
package consumer

import framework.macros.localSpec
import framework.macros.Formal.*
import Specs.*

class MemUnit extends Module:
  localSpec(contMem)            // anchor the module (Scala 3 form of @LocalSpec)

  val reqOut = new MemReq
  localSpec(intfReqOut)         // anchor the port

  var outstanding    = 0
  val maxOutstanding = 4

  val bounded = assertProperty(propBounded) { outstanding <= maxOutstanding }
  assert(bounded, "txn table overflow")

object Main:
  def main(args: Array[String]): Unit =
    val _ = (Specs.contMem, Specs.intfReqOut, Specs.paramAddrW, Specs.bndMemReq, Specs.propBounded)
    new MemUnit
    println("[scala3-demo] specs emitted at run time")
