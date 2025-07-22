package your_project.specs

import framework.macros.SpecEmit.spec
import framework.spec.Spec._

object BundleSpecs {
  val fetchReqBundle = spec {
    BUNDLE("BND_FETCH_REQ_BUNDLE")
      .desc("Simple fetch request bundle")
      .entry("pc", "Program counter [type: UInt, width: 32]")
      .entry("valid", "Request valid [type: Bool]")
      .build()
  }

  val busInterface = spec {
    INTERFACE("INTF_FETCH_BUS")
      .desc("Bus interface using fetch request bundle")
      .entry("request", "Outgoing request").has("BND_FETCH_REQ_BUNDLE")
      .build()
  }
}
