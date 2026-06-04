package streamproc.design.top

import streamproc.hdl._
import framework.macros.localSpec
import streamproc.design._
import streamproc.design.modules._
import streamproc.specs.top.TopSpecs.*

class StreamProcessorTop(c: SPConfig) extends Module:
  localSpec(contStreamProcessor)
  val in = Flipped(Decoupled(new StreamBeat(c)))
  localSpec(intfStreamIn)
  val out = Decoupled(new StreamBeat(c))
  localSpec(intfStreamOut)

  val ingress    = new IngressBuffer(c)
  val framer     = new Framer(c)
  val classifier = new Classifier(c)
  val egress     = new EgressBuffer(c)
  val shaper     = if c.enableShaper then Some(new Shaper(c)) else None

  localSpec(funcPipeline)

// Drives spec emission (Scala 3 emits specs at run time) and exercises the graph.
object Main:
  def main(args: Array[String]): Unit =
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
    new StreamProcessorTop(SPConfig())
    println("[stream-processor-s3] specs emitted at run time")
