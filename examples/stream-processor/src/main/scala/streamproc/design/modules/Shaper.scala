package streamproc.design.modules

import chisel3._
import chisel3.util._
import framework.macros.localSpec
import framework.macros.Formal._
import streamproc.design._
import streamproc.specs.modules.ShaperSpecs._

/** Token-bucket rate shaper: one token refilled per cycle up to the burst size;
  * each forwarded beat consumes a token. */
class Shaper(c: SPConfig) extends Module {
  localSpec(contShaper)

  val in = localSpec(intfShaperIn, IO(Flipped(Decoupled(new TaggedBeat(c)))))
  val out = localSpec(intfShaperOut, IO(Decoupled(new TaggedBeat(c))))

  val tokens = RegInit(c.shaperBurst.U(log2Ceil(c.shaperBurst + 1).W))
  localSpec(funcRateLimit)
  val haveToken = tokens =/= 0.U

  out.valid := in.valid && haveToken
  in.ready  := out.ready && haveToken
  out.bits  := in.bits

  val refill  = (tokens < c.shaperBurst.U) || c.injectShaperBug.B // BUG: refills past the cap
  val consume = out.fire
  when(consume && !refill) { tokens := tokens - 1.U }
    .elsewhen(!consume && refill) { tokens := tokens + 1.U }

  assert(assertProperty(propTokenBounded) { tokens <= c.shaperBurst.U }, "[PROP_TOKEN_BOUNDED] shaper token count exceeded burst")
  cover(coverProperty(covTokensDrained) { tokens === 0.U }, "[COV_TOKENS_DRAINED] shaper ran out of tokens")
}
