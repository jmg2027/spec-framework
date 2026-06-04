// spec-core/src/main/scala/framework/spec/SpecSim.scala
// -----------------------------------------------------------------------------
//  Spec-driven simulation.  The SAME PROPERTY / COVERAGE spec objects that the
//  RTL binds with `assertProperty` / `coverProperty` are reused in a testbench
//  as runtime checkers and cover points, so the spec is the single source of the
//  verification plan. Results are written to `VerifIndex.json`, which SpecCheck
//  folds back into the compliance report (each property gains a sim/cover status).
//
//  This is JVM-only: a `Bench` ticks cycles while a model (or, with a simulator
//  backend, a peeked DUT) supplies the boolean each cycle. No external tool
//  needed; swap the model for `peek` calls to drive the real RTL.
// -----------------------------------------------------------------------------
package framework.spec

import scala.collection.mutable
import upickle.default.{macroRW, ReadWriter}
import java.nio.file.{Files, Path}

/** One spec's accumulated verification evidence over a run. */
final case class VerifResult(
    id: String,
    kind: String,        // "assert" | "cover"
    cycles: Int,         // cycles the checker/cover was evaluated
    fails: Int,          // assert: cycles the condition was false
    firstFailCycle: Int, // assert: first failing cycle, -1 if none
    hits: Int,           // cover: cycles the condition was true
    backend: String = "model", // which engine produced this (model / verilator / sby / …)
) {
  def status: String = kind match {
    case "assert"                   => if (cycles == 0) "unchecked" else if (fails == 0) "passed" else "failed"
    case "cover" | "functional"     => if (cycles == 0) "unchecked" else if (hits > 0) "covered" else "uncovered"
    case _                          => "unchecked"
  }
}
object VerifResult { implicit val rw: ReadWriter[VerifResult] = macroRW }

/** A testbench clock + result accumulator. Create one, step it each cycle while
  * calling `checkProperty` / `coverPoint`, then `writeIndex`. */
final class Bench(backend: String = "model") {
  private var _cycle = 0
  private val results = mutable.LinkedHashMap.empty[String, VerifResult]

  def cycle: Int       = _cycle
  def step(): Unit     = { _cycle += 1 }
  def run(n: Int)(body: Int => Unit): Unit = { var i = 0; while (i < n) { body(i); step(); i += 1 } }

  /** Record an assertion verdict directly (for a backend — e.g. verilator/sby —
    * that checks the property itself rather than per-cycle in Scala). */
  def recordAssert(spec: HardwareSpecification, passed: Boolean, atCycle: Int, cycles: Int): Unit =
    results(spec.id) = VerifResult(spec.id, "assert", cycles, if (passed) 0 else 1, if (passed) -1 else atCycle, 0, backend)
  def recordCover(spec: HardwareSpecification, hits: Int, cycles: Int): Unit =
    results(spec.id) = VerifResult(spec.id, "cover", cycles, 0, -1, hits, backend)

  /** Evaluate `cond` for PROPERTY `spec` this cycle; a false is a failure. */
  def checkProperty(spec: HardwareSpecification)(cond: => Boolean): Unit = {
    val r  = results.getOrElse(spec.id, VerifResult(spec.id, "assert", 0, 0, -1, 0, backend))
    val ok = cond
    results(spec.id) = r.copy(
      cycles = r.cycles + 1,
      fails = r.fails + (if (ok) 0 else 1),
      firstFailCycle = if (!ok && r.firstFailCycle < 0) _cycle else r.firstFailCycle,
    )
  }

  /** Record whether COVERAGE `spec`'s `cond` is hit this cycle. */
  def coverPoint(spec: HardwareSpecification)(cond: => Boolean): Unit = {
    val r = results.getOrElse(spec.id, VerifResult(spec.id, "cover", 0, 0, -1, 0, backend))
    results(spec.id) = r.copy(cycles = r.cycles + 1, hits = r.hits + (if (cond) 1 else 0))
  }

  /** Functional coverage: was this FUNCTION / INTERFACE spec exercised (e.g. did
    * the interface fire / the function's logic run) this cycle. The obligations
    * are auto-derived from the spec graph (see SpecCheck), so you do not hand-write
    * a coverage plan — you only supply the activity bit. */
  def functional(spec: HardwareSpecification)(active: => Boolean): Unit = {
    val r = results.getOrElse(spec.id, VerifResult(spec.id, "functional", 0, 0, -1, 0, backend))
    results(spec.id) = r.copy(cycles = r.cycles + 1, hits = r.hits + (if (active) 1 else 0))
  }
  /** Auto-instantiate functional cover points for every FUNCTION/INTERFACE in
    * `specs`, using `active(id)` as the per-node activity. */
  def autoFunctional(specs: Seq[HardwareSpecification])(active: String => Boolean): Unit =
    specs.foreach { s =>
      val c = s.category.toString
      if (c.contains("FUNCTION") || c.contains("INTERFACE")) functional(s)(active(s.id))
    }

  def all: List[VerifResult] = results.values.toList

  /** Write VerifIndex.json into `dir` (defaults to the spec.meta.dir). */
  def writeIndex(dir: Path): Unit = {
    Files.createDirectories(dir)
    Files.write(dir.resolve("VerifIndex.json"), upickle.default.write(all, indent = 2).getBytes)
  }
  def writeIndex(): Unit =
    writeIndex(java.nio.file.Paths.get(sys.props.getOrElse("spec.meta.dir", ".")))
}
