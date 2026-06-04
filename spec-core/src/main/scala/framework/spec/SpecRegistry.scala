// src/main/scala/framework/spec/SpecRegistry.scala
package framework.spec

import scala.collection.mutable.ListBuffer

/**
 * SpecRegistry: an in-memory runtime registry of [[HardwareSpecification]]
 * definitions, populated as spec `val`s are evaluated. It is a convenience for
 * runtime inspection/tests only — the authoritative export pipeline is
 * file-based (`.spec`/`.tag` artefacts aggregated by the sbt plugin /
 * [[SpecCheck]]), so nothing here is read during export.
 *
 * (The tag side and verbose debug logging were removed: tags are never
 * registered here — they are emitted straight to `.tag` files by the macros.)
 */
object SpecRegistry {
  private[spec] val specBuf: ListBuffer[HardwareSpecification] =
    ListBuffer.empty[HardwareSpecification]

  /** Register a hardware specification definition. */
  def addSpec(spec: HardwareSpecification): Unit = { specBuf += spec; () }

  /** All specifications registered so far. */
  def allSpecs: Seq[HardwareSpecification] = specBuf.toSeq

  /** Reset the registry (for tests). */
  def clear(): Unit = specBuf.clear()
}
