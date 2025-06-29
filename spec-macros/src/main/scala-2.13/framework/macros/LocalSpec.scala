// spec-macros/src/main/scala-2.13/framework/macros/LocalSpec.scala
package framework.macros

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

@compileTimeOnly("enable -Ymacro-annotations")
class LocalSpec(id: String) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro LocalSpec.impl
}

object LocalSpec {

  /*─────────────────────────────────────────────────*/
  /*  Macro implementation                           */
  /*─────────────────────────────────────────────────*/
  def impl(c: Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._

    /*----- utilities -----*/
    def abort(msg: String) = c.abort(c.enclosingPosition, msg)

    /*----- extract annotation arg -----*/
    val Literal(Constant(specId: String)) =
      c.prefix.tree match {
        case q"new $_(${arg})" => arg
        case _                 => abort("@LocalSpec requires a string literal ID")
      }

    /*----- duplicate‑id guard (per compile run) -----*/
    LocalSpec.seenIds.synchronized {
      if (!LocalSpec.seenIds.add(specId))
        abort(s"Duplicate @LocalSpec id '$specId' in same compilation run")
    }

    /*----- build Tag value + side‑effect statement -----*/
    val pos = c.enclosingPosition
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

    /*──────────────────────────────────────────────*/
    /*  helper: ensure companion object exists      */
    /*──────────────────────────────────────────────*/
    def ensureCompanion(cls: ClassDef, maybeObj: Option[ModuleDef]): (ClassDef, ModuleDef) = {
      val q"$mods class $tname[..$tps] $ctorMods(...$pss) extends {..$early} with ..$parents { $self => ..$body }" = cls
      maybeObj match {
        // companion already present – prepend tagStmt
        case Some(q"$omods object $oname extends {..$oearly} with ..$oparents { $oself => ..$obody }") =>
          val newObj =
            q"""$omods object $oname extends {..$oearly} with ..$oparents { $oself =>
                  $tagStmt; ..$obody
                }"""
          (cls, newObj)

        // no companion – create one
        case None =>
          val objName = TermName(tname.decodedName.toString)
          val newObj =
            q"""object $objName {
                  $tagStmt
                }"""
          (cls, newObj)
      }
    }

    /*──────────────────────────────────────────────*/
    /*  pattern‑match annottees                     */
    /*──────────────────────────────────────────────*/
    annottees.toList match {

      /*----- case 1: class [, companion] -----*/
      case (cls: ClassDef) :: rest =>
        val maybeComp = rest.collectFirst { case m: ModuleDef => m }
        val (newCls, newObj) = ensureCompanion(cls, maybeComp)
        val other            = rest.filterNot(_.isInstanceOf[ModuleDef])
        q"..${newCls +: newObj +: other}"

      /*----- case 2: companion object -----*/
      case (q"$mods object $name extends {..$early} with ..$parents { $self => ..$stats }") :: Nil =>
        q"""$mods object $name extends {..$early} with ..$parents { $self =>
              $tagStmt; ..$stats
            }"""

      /*----- case 3: val definition -----*/
      case (q"$mods val $n: $tpt = $rhs") :: Nil =>
        q"$mods val $n: $tpt = { $tagStmt; $rhs }"

      /*----- case 4: def definition -----*/
      case (q"$mods def $n[..$tps](...$pss): $tpt = $rhs") :: Nil =>
        q"$mods def $n[..$tps](...$pss): $tpt = { $tagStmt; $rhs }"

      case _ =>
        abort("@LocalSpec can only annotate class, object, val, or def definitions")
    }
  }

  /*──────────────────────────────────────────────*/
  /*  per‑compiler‑run duplicate‑id set           */
  /*──────────────────────────────────────────────*/
  private val seenIds = scala.collection.mutable.HashSet.empty[String]
}
