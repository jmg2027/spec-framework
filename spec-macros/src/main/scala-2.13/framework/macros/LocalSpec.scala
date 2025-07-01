// -----------------------------------------------------------------------------
//  @LocalSpec  – compile-time macro (now supports val/obj argument)
//  Location: spec-macros/src/main/scala-2.13/framework/macros/LocalSpec.scala
//  Target: Scala 2.13 macro-annotation environment
// -----------------------------------------------------------------------------

package framework.macros

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

import _root_.framework.spec.{Tag, MetaFile, HardwareSpecification}

@compileTimeOnly("enable -Ymacro-annotations")
final class LocalSpec(arg: Any) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro LocalSpec.impl
}

object LocalSpec {
  def impl(c: Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._

    def abort(msg: String) = c.abort(c.enclosingPosition, msg)

    // 1) Extract the spec-ID (string literal or HardwareSpecification reference)
    val specId: String = c.prefix.tree match {
      case q"new $_(${Literal(Constant(id: String))})" => id

      case q"new $_($expr)" =>
        val tpe = c.typecheck(expr).tpe
        val expected = c.typeOf[_root_.framework.spec.HardwareSpecification]
        if (!(tpe <:< expected))
          c.abort(expr.pos, s"@LocalSpec expected String or HardwareSpecification, got: " + tpe)

        val evaluated = try {
          c.eval(c.Expr[HardwareSpecification](c.untypecheck(expr.duplicate)))
        } catch {
          case e: Throwable =>
            c.abort(expr.pos, s"@LocalSpec: evaluation failed: ${e.getMessage}")
        }

        evaluated.id
    }

    val pos = c.enclosingPosition
    val scalaDeclarationPath: String = c.internal.enclosingOwner.fullName + {
      annottees.head match {
        case DefDef(_, name, _, _, _, _) => "." + name.toString
        case ValDef(_, name, _, _)       => "." + name.toString
        case ClassDef(_, name, _, _)     => "." + name.toString
        case ModuleDef(_, name, _)       => "." + name.toString
        case _                           => ""
      }
    }

    val tag = Tag(
      id                      = specId,
      scalaDeclarationPath    = scalaDeclarationPath,
      fullyQualifiedModuleName= c.internal.enclosingOwner.fullName,
      hardwareInstancePath    = "", // Optional, if unused
      srcFile                 = pos.source.path,
      line                    = pos.line,
      column                  = pos.column
    )


    MetaFile.writeTag(tag)
    c.info(pos, s"[LocalSpec] emitted .tag for '${tag.id}'", force = true)

    annottees.head
  }
}
