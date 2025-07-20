// StandaloneSpec.scala
// Standalone version of the Spec framework - no dependencies required
// Just copy this file to your project and use it directly

package framework.specs

// Dummy types to make compilation work without the full framework
case class Capability(name: String)

case class SpecBuilder(id: String, desc: String = "") {
  def desc(d: String): SpecBuilder = this.copy(desc = d)
  def is(other: Any*): SpecBuilder = this
  def has(other: Any*): SpecBuilder = this
  def uses(other: Any*): SpecBuilder = this
  def status(s: String): SpecBuilder = this
  def entry(key: String, value: String = ""): SpecBuilder = this
  def table(tableType: String, content: String): SpecBuilder = this
  def draw(drawType: String, content: String): SpecBuilder = this
  def code(language: String, content: String): SpecBuilder = this
  def code(content: String): SpecBuilder = this
  def note(n: String): SpecBuilder = this
  def build(): Unit = {
    // In standalone mode, just print or do nothing
    // Users can override this behavior if needed
    println(s"[Spec] Built spec '$id': $desc")
  }
}

object Spec {
  // Entry points per category - same API as the full framework
  def CONTRACT(id: String): SpecBuilder = SpecBuilder(id)
  def FUNCTION(id: String): SpecBuilder = SpecBuilder(id)
  def PROPERTY(id: String): SpecBuilder = SpecBuilder(id)
  def COVERAGE(id: String): SpecBuilder = SpecBuilder(id)
  def INTERFACE(id: String): SpecBuilder = SpecBuilder(id)
  def PARAMETER(id: String): SpecBuilder = SpecBuilder(id)
  def CAPABILITY(id: String): SpecBuilder = SpecBuilder(id)
  def RAW(id: String, prefix: String): SpecBuilder = SpecBuilder(id, prefix)
}

// Dummy annotation for compatibility
class LocalSpec(spec: Any) extends scala.annotation.StaticAnnotation

object SpecEmit {
  // Dummy spec function that just returns the input
  def spec[T](body: T): T = body
  def emit[T](body: T): T = body
}
