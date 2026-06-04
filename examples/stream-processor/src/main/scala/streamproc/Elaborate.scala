package streamproc

import circt.stage.ChiselStage
import streamproc.design._
import streamproc.design.top.StreamProcessorTop

/** Elaborates the design (producing a FIRRTL artefact) and forces every spec
  * object to initialise. On Scala 3 the latter is what emits the `.spec` files
  * (specs are emitted at run time); on Scala 2 it is a harmless re-emit. */
object Elaborate {
  private def touchSpecs(): Unit = {
    import streamproc.specs.params.ParamSpecs
    import streamproc.specs.bundles.BundleSpecs
    import streamproc.specs.modules._
    import streamproc.specs.top.TopSpecs
    val _ = Seq[Any](
      ParamSpecs.paramDataBytes, BundleSpecs.bndStreamBeat,
      IngressBufferSpecs.contIngressBuffer, FramerSpecs.contFramer,
      ClassifierSpecs.contClassifier, ShaperSpecs.contShaper,
      EgressBufferSpecs.contEgressBuffer, TopSpecs.contStreamProcessor,
    )
  }

  def main(args: Array[String]): Unit = {
    touchSpecs()
    val c       = SPConfig()
    val chirrtl = ChiselStage.emitCHIRRTL(new StreamProcessorTop(c))
    val out     = new java.io.File("target/StreamProcessorTop.fir")
    java.nio.file.Files.write(out.toPath, chirrtl.getBytes)
    println(s"[stream-processor] elaborated StreamProcessorTop → ${out.getAbsolutePath}")
  }
}
