// spec-core/src/main/scala/framework/spec/Typed.scala
// -----------------------------------------------------------------------------
//  Typed bundle specifications  ("타입화")
// -----------------------------------------------------------------------------
//  The original DSL describes a BUNDLE's fields as free-text strings:
//
//      BUNDLE("BND_FETCH_REQUEST").entry("addr", "UInt(pcWidth)")...
//
//  Nothing connects that string to the *actual* hardware bundle, so the spec and
//  the implementation drift apart silently (e.g. the RTL renames `addr`, adds a
//  `size` field, drops `epoch`, and the spec never notices).
//
//  `bundleSpec[T]` closes that gap by binding the spec to the implementation type
//  `T` and declaring fields through **typed selectors**:
//
//      bundleSpec[FetchRequest]("BND_FETCH_REQUEST").desc("…")
//        .field("addr",  _.addr)     // _.addr must exist on FetchRequest …
//        .field("txnId", _.txnId)    // … or the spec file does NOT compile
//        .build()
//
//  Two guarantees fall out of the Scala type system, for free:
//    1. **Existence** — `_.addr` is a `FetchRequest => F`. If the field is renamed
//       or removed, the selector fails to type-check and the *spec* stops
//       compiling. Field drift becomes a compile error, not a runtime surprise.
//    2. **Honest types** — the recorded field type is read from the selector's
//       result type `F`, i.e. derived from the RTL. The spec cannot misreport a
//       type, because it never names the type itself.
//
//  `build()` additionally reflects over `T` to find public fields that were
//  *not* declared and records them as `undeclared-fields:` so the checker can
//  flag spec incompleteness (a soft warning, since omission is not a type error).
// -----------------------------------------------------------------------------
package framework.spec

import scala.reflect.runtime.universe.TypeTag
import scala.util.Try

object Typed {

  /** Entry point: start a typed BUNDLE spec bound to implementation type `T`. */
  def bundleSpec[T](id: String)(implicit tt: TypeTag[T]): BundleStage0[T] =
    new BundleStage0[T](id, tt)

  /** Stage requiring a description (mirrors the staged `Spec` DSL). */
  final class BundleStage0[T] private[Typed] (id: String, tt: TypeTag[T]) {
    def desc(d: String): BundleSpecBuilder[T] =
      new BundleSpecBuilder[T](id, d, tt, Nil, Set.empty, Nil)
  }

  final class BundleSpecBuilder[T] private[Typed] (
      id: String,
      desc: String,
      tt: TypeTag[T],
      fields: List[(String, String)],
      usesIds: Set[String],
      notes: List[String],
  ) {

    /**
     * Declare a field by a typed selector. `sel` is never invoked — it only has
     * to type-check, so referencing a missing/renamed field is a compile error.
     * The field's type label is taken from `F`, i.e. from the implementation.
     */
    def field[F](name: String, sel: T => F)(implicit ft: TypeTag[F]): BundleSpecBuilder[T] =
      new BundleSpecBuilder[T](id, desc, tt, fields :+ (name -> typeLabel(ft)), usesIds, notes)

    /** Reference parameter specs by id (kept as ids, like the core DSL relations,
      * so this stays evaluable inside the compile-time `spec { … }` macro). */
    def uses(ids: String*): BundleSpecBuilder[T] =
      new BundleSpecBuilder[T](id, desc, tt, fields, usesIds ++ ids, notes)

    def note(n: String): BundleSpecBuilder[T] =
      new BundleSpecBuilder[T](id, desc, tt, fields, usesIds, notes :+ n)

    def build(): HardwareSpecification = {
      require(!id.contains(" "), s"Spec ID '$id' must not contain spaces")
      require(desc.nonEmpty, "description must be provided via desc")

      val declared   = fields.map(_._1).toSet
      val undeclared = actualFields(tt).filterNot(declared.contains)
      val allNotes =
        ("typed-bundle" :: notes) ++
          (if (undeclared.nonEmpty) List("undeclared-fields: " + undeclared.mkString(", ")) else Nil)

      val spec = HardwareSpecification(
        id          = id,
        category    = SpecCategory.BUNDLE,
        description = desc,
        uses        = usesIds,
        lists       = fields,
        notes       = allNotes,
      )
      MetaFile.writeSpec(spec)
      SpecRegistry.addSpec(spec)
      spec
    }
  }

  // ---------------------------------------------------------------------------
  // Reflection helpers. Both are defensive: a typed bundle spec must keep
  // compiling even if the runtime mirror cannot fully resolve a type (e.g. an
  // exotic generic), in which case we degrade gracefully rather than abort.
  // ---------------------------------------------------------------------------

  /** Human-readable label for the field type, derived from the implementation. */
  private def typeLabel(tt: TypeTag[_]): String =
    Try(tt.tpe.toString).getOrElse(tt.toString).replaceFirst("^.*\\.", "")

  /** Public nullary accessors of `T` (works for case classes and `val`-bundles). */
  private def actualFields(tt: TypeTag[_]): List[String] =
    Try {
      import scala.reflect.runtime.universe._
      tt.tpe.members.collect {
        case m: MethodSymbol if m.isGetter && m.isPublic => m.name.toString.trim
      }.toList
    }.getOrElse(Nil)
}
