package framework.spec
import scala.collection.mutable
import java.nio.file.{Files, Path}

/**
 * SpecRegistry: Central registry for all HardwareSpecification and Tag objects.
 *
 * - addSpec/addTag: Register new specs and tags
 * - allSpecs/all: Retrieve all registered specs and tags
 *
 * This object is used by macros and plugins to collect and serialize all spec/RTL metadata.
 * All additions are in-memory and not thread-safe (intended for single-threaded SBT/compile use).
 */
object SpecRegistry {
  // Buffer for all Tag objects (LocalSpec macro registration)
  private val tagBuf = mutable.ListBuffer.empty[Tag]
  /** Register a Tag (called by @LocalSpec macro) */
  def addTag(tag: Tag): Unit = tagBuf += tag
  /** Get all registered Tag objects */
  def all: List[Tag]   = tagBuf.toList

  // Buffer for all HardwareSpecification objects (SpecBuilder registration)
  private val specBuf = mutable.ListBuffer.empty[HardwareSpecification]
  /** Register a HardwareSpecification (called by SpecBuilder) */
  def addSpec(spec: HardwareSpecification): Unit = specBuf += spec
  /** Get all registered HardwareSpecification objects */
  def allSpecs: List[HardwareSpecification] = specBuf.toList

  // Dump all Tag objects as JSON (for plugin or SBT task use)
  private lazy val jsonRepr: String = upickle.default.write(all, indent = 2)
  def dumpTo(path: Path): Unit = Files.write(path, jsonRepr.getBytes)
}
