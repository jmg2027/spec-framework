package framework.macros

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

import _root_.framework.spec.{SpecRegistry, Tag}

/**
 * LocalSpec annotation for tagging RTL/Scala code with a hardware specification ID.
 * This macro injects code at compile time to register a Tag in the SpecRegistry.
 *
 * Usage: @LocalSpec("MY_SPEC_ID")
 * - Only string literals are allowed as arguments.
 * - The string must match the id field of a HardwareSpecification defined elsewhere.
 *
 * This macro can annotate class, object, val, or def definitions.
 */
@compileTimeOnly("enable -Ymacro-annotations")
class LocalSpec(specId: String) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro LocalSpec.impl
}

object LocalSpec {
  /**
   * Macro implementation for @LocalSpec.
   * - Extracts the string literal argument (specId).
   * - Captures source file, line, and column.
   * - Injects a call to SpecRegistry.addTag with a Tag containing all metadata.
   * - Supports annotation on class, object, val, and def.
   */
  def impl(c: Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._

    /** Abort macro expansion with a message. */
    def abort(msg: String) = c.abort(c.enclosingPosition, msg)

    // Extract the string literal argument from @LocalSpec("MY_SPEC_ID")
    val q"new $_(${idLit: c.Tree})" = c.prefix.tree
    val specId: String = idLit match {
      case Literal(Constant(s: String)) => s
      case _ => abort("@LocalSpec must be used with a string literal, e.g. @LocalSpec(\"QUEUE_FUNC_001\")")
    }

    // Capture source file, line, and column for traceability
    val srcPath = c.enclosingPosition.source.path
    val line = c.enclosingPosition.line
    val column = c.enclosingPosition.column

    // Code to register a Tag in the SpecRegistry at the annotation site
    // Placeholders for fullyQualifiedModuleName and hardwareInstancePath are used;
    // these will be filled in later by FIRRTL or other transforms.
    val tagAdditionCodeTemplate =
      q"""
         _root_.framework.spec.SpecRegistry.addTag(
           _root_.framework.spec.Tag(
             id = $specId,
             fullyQualifiedModuleName = "PLACEHOLDER_MODULE",
             hardwareInstancePath = "PLACEHOLDER_PATH",
             srcFile = $srcPath,
             line = $line,
             column = $column
           )
         )
       """

    /**
     * Inject Tag registration at the start of a class body.
     */
    def handleClass(cls: c.Tree, mods: Modifiers, tname: TypeName, tparams: List[TypeDef], ctorMods: Modifiers, paramss: List[List[ValDef]], earlydefns: List[Tree], parents: List[Tree], self: ValDef, bodyStats: List[Tree]): c.Tree = {
      q"""$mods class $tname[..$tparams] $ctorMods(...$paramss) extends {..$earlydefns} with ..$parents { $self => ..${tagAdditionCodeTemplate +: bodyStats} }"""
    }

    /**
     * Inject Tag registration at the start of an object body.
     */
    def handleObject(obj: c.Tree, mods: Modifiers, tname: TermName, earlydefns: List[Tree], parents: List[Tree], self: ValDef, stats: List[Tree]): c.Tree = {
      q"""$mods object $tname extends { ..$earlydefns } with ..$parents { $self => ..${tagAdditionCodeTemplate +: stats} }"""
    }

    /**
     * Inject Tag registration before the initialization of a val.
     */
    def handleVal(valDef: c.Tree, mods: Modifiers, tname: TermName, tpt: Tree, expr: Tree): c.Tree = {
      val newExprWithTag = q"{ $tagAdditionCodeTemplate; $expr }"
      q"$mods val $tname: $tpt = $newExprWithTag"
    }

    /**
     * Inject Tag registration at the start of a def body.
     */
    def handleDef(defDef: c.Tree, mods: Modifiers, tname: TermName, tparams: List[TypeDef], paramss: List[List[ValDef]], tpt: Tree, expr: Tree): c.Tree = {
      val newBodyWithTag = q"{ $tagAdditionCodeTemplate; $expr }"
      q"$mods def $tname[..$tparams](...$paramss): $tpt = $newBodyWithTag"
    }

    // Pattern match on the annotated code and inject Tag registration accordingly
    annottees match {
      case (cls @ q"$mods class $tname[..$tparams] $ctorMods(...$paramss) extends {..$earlydefns} with ..$parents { $self => ..$bodyStats }") :: tail =>
        handleClass(cls, mods, tname, tparams, ctorMods, paramss, earlydefns, parents, self, bodyStats)
      case (obj @ q"$mods object $tname extends { ..$earlydefns } with ..$parents { $self => ..$stats }") :: tail =>
        handleObject(obj, mods, tname, earlydefns, parents, self, stats)
      case (valDef @ q"$mods val $tname: $tpt = $expr") :: tail =>
        handleVal(valDef, mods, tname, tpt, expr)
      case (defDef @ q"$mods def $tname[..$tparams](...$paramss): $tpt = $expr") :: tail =>
        handleDef(defDef, mods, tname, tparams, paramss, tpt, expr)
      case _ => abort("@LocalSpec can only annotate class, object, val, or def definitions.")
    }
  }
}