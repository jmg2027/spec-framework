// spec-macros/src/main/scala-2.13/framework/macros/LocalSpec.scala
// Macro annotation for tagging hardware modules/specs with a spec ID (@LocalSpec)
// Emits Tag metadata at compile time for plugin aggregation and registry.
package framework.macros

import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

/**
 * LocalSpec: Macro annotation for tagging classes, objects, vals, or defs with
 * a hardware spec ID.
 *
 * Usage:
 * @LocalSpec("MY_SPEC_ID")
 *   class MyModule { ... }
 *
 * This macro will:
 *   - Enforce that the argument is a string literal (the spec ID, must be
 *     unique per compile run)
 *   - Prevent duplicate IDs within the same compilation run (compile-time
 *     error)
 *   - At compile time, emit a Tag metadata file (for plugin aggregation)
 *   - Register the tag in SpecRegistry for runtime/debug use
 *   - Support annotation of class, object, val, or def (injects side effect in
 *     companion or body)
 *
 * See design/build.sbt for required macro-annotation settings.
 */
@compileTimeOnly("enable -Ymacro-annotations")
class LocalSpec(id: String) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro LocalSpec.impl
}

/**
 * Macro implementation object for @LocalSpec. Handles annotation argument
 * extraction, duplicate ID checking, tag emission, and code transformation.
 * Ensures that every annotated entity emits a Tag at compile time and is
 * registered for later aggregation.
 */
object LocalSpec {

  /*───────────────────────────────────────────────────────────────*/
  /*  Macro implementation: argument extraction, duplicate check,  */
  /*  tag emission, and code transformation for all supported      */
  /*  annotation targets.                                          */
  /*───────────────────────────────────────────────────────────────*/
  /**
   * Macro implementation for @LocalSpec.
   *
   *   - Extracts the string literal argument (spec ID)
   *   - Checks for duplicate IDs in the same compile run (compile-time error on
   *     duplicate)
   *   - Builds a Tag value and emits it (to SpecRegistry and as a .tag file)
   *   - Rewrites the annotated code to include the tag emission as a side
   *     effect
   *
   * Supports annotation of class, object, val, or def. Injects tag emission in
   * the companion object or body.
   *
   * @param c
   *   Macro context (whitebox)
   * @param annottees
   *   The annotated definitions (class, object, val, or def)
   * @return
   *   The transformed code with tag emission side effect
   */
  def impl(c: Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._

    // Utility: abort macro expansion with a message (compile-time error)
    def abort(msg: String) = c.abort(c.enclosingPosition, msg)

    // Extract the string literal argument (spec ID) from the annotation (must be a string literal)
    val Literal(Constant(specId: String)) =
      c.prefix.tree match {
        case q"new $_(${arg})" => arg
        case _                 => abort("@LocalSpec requires a string literal ID")
      }

    // Prevent duplicate IDs within the same compilation run (fail fast on duplicate)
    LocalSpec.seenIds.synchronized {
      if (!LocalSpec.seenIds.add(specId))
        abort(s"Duplicate @LocalSpec id '$specId' in same compilation run")
    }

    // Build the Tag value and the side-effect statement (register and emit tag at compile time)
    val pos     = c.enclosingPosition
    val tagTree =
      q"""
        _root_.framework.spec.Tag(
          id                       = $specId,
          fullyQualifiedModuleName = "PLACEHOLDER_MODULE",
          hardwareInstancePath     = "",
          srcFile                  = ${pos.source.path},
          line                     = ${pos.line},
          column                   = ${pos.column}
        )
      """

    val tagStmt =
      q"""
        _root_.framework.spec.SpecRegistry.addTag($tagTree);
        _root_.framework.spec.MetaFile.writeTag($tagTree)
      """

    // Helper: ensure a companion object exists for a class, and inject tag emission into it (or create one)
    def ensureCompanion(
      cls: ClassDef,
      maybeObj: Option[ModuleDef],
    ): (ClassDef, ModuleDef) = {
      val q"$mods class $tname[..$tps] $ctorMods(...$pss) extends {..$early} with ..$parents { $self => ..$body }" =
        cls
      maybeObj match {
        // companion already present – prepend tagStmt
        case Some(
              q"$omods object $oname extends {..$oearly} with ..$oparents { $oself => ..$obody }",
            ) =>
          val newObj =
            q"""$omods object $oname extends {..$oearly} with ..$oparents { $oself =>
                  $tagStmt; ..$obody
                }"""
          (cls, newObj)

        // no companion – create one
        case None =>
          val objName = TermName(tname.decodedName.toString)
          val newObj  =
            q"""object $objName {
                  $tagStmt
                }"""
          (cls, newObj)
      }
    }

    // Pattern-match on the annotated definition and rewrite as needed for all supported targets
    annottees.toList match {

      // Case 1: class (with or without companion object)
      case (cls: ClassDef) :: rest                                                                 =>
        val maybeComp        = rest.collectFirst { case m: ModuleDef => m }
        val (newCls, newObj) = ensureCompanion(cls, maybeComp)
        val other            = rest.filterNot(_.isInstanceOf[ModuleDef])
        q"..${newCls +: newObj +: other}"

      // Case 2: companion object only (inject tag emission at top)
      case (q"$mods object $name extends {..$early} with ..$parents { $self => ..$stats }") :: Nil =>
        q"""$mods object $name extends {..$early} with ..$parents { $self =>
              $tagStmt; ..$stats
            }"""

      // Case 3: val definition (inject tag emission in value body)
      case (q"$mods val $n: $tpt = $rhs") :: Nil                                                   =>
        q"$mods val $n: $tpt = { $tagStmt; $rhs }"

      // Case 4: def definition (inject tag emission in method body)
      case (q"$mods def $n[..$tps](...$pss): $tpt = $rhs") :: Nil                                  =>
        q"$mods def $n[..$tps](...$pss): $tpt = { $tagStmt; $rhs }"

      case _ =>
        abort(
          "@LocalSpec can only annotate class, object, val, or def definitions",
        )
    }
  }

  // Per-compiler-run set to track seen IDs and prevent duplicates (reset on each compile run)
  private val seenIds = scala.collection.mutable.HashSet.empty[String]
}
