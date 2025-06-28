// spec-macros/src/main/scala-2.13/framework/macros/LocalSpec.scala

package framework.macros

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

import _root_.framework.spec.{SpecRegistry, Tag}

@compileTimeOnly("enable -Ymacro-annotations")
class LocalSpec(specId: String) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro LocalSpec.impl
}

object LocalSpec {
  def impl(c: Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._

    def abort(msg: String) = c.abort(c.enclosingPosition, msg)

    val Literal(Constant(specId: String)) =
      c.prefix.tree match { case q"new $_(${arg})" => arg; case _ => abort("...") }

    // duplicate-id guard (per compiler run)
    LocalSpec.seenIds.synchronized {
      if (!LocalSpec.seenIds.add(specId))
        abort(s"Duplicate @LocalSpec id '$specId' in same compilation run")
    }

    val pos   = c.enclosingPosition
    val tagQs =
      q"""
        _root_.framework.spec.Tag(
          id = $specId,
          fullyQualifiedModuleName = "PLACEHOLDER_MODULE",
          hardwareInstancePath     = "",
          srcFile  = ${pos.source.path},
          line     = ${pos.line},
          column   = ${pos.column}
        )
      """

    val tagStmt = q"{ _root_.framework.spec.SpecRegistry.addTag($tagQs); _root_.framework.spec.MetaFile.writeTag($tagQs) }"

    def wrap(tree: Tree): Tree = tree match {
      case q"$mods val $name: $tpt = $rhs" =>
        q"$mods val $name: $tpt = { $tagStmt; $rhs }"
      case q"$mods def $name[..$tps](...$paramss): $tpt = $rhs" =>
        q"$mods def $name[..$tps](...$paramss): $tpt = { $tagStmt; $rhs }"
      case q"$mods object $name extends { ..$early } with ..$parents { $self => ..$body }" =>
        q"$mods object $name extends { ..$early } with ..$parents { $self => $tagStmt; ..$body }"
      case q"$mods class $name[..$tps] $ctorMods(...$pss) extends {..$early} with ..$parents { $self => ..$body }" =>
        q"$mods class $name[..$tps] $ctorMods(...$pss) extends {..$early} with ..$parents { $self => $tagStmt; ..$body }"
      case other => abort("@LocalSpec can only annotate class, object, val, or def")
    }

    val transformed = annottees.map(wrap)
    q"..$transformed"
  }

  // per-compiler-run mutable state (safe)
  private val seenIds = scala.collection.mutable.HashSet.empty[String]
}
