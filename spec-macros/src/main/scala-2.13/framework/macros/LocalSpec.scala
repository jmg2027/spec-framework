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
import _root_.framework.spec.SpecIndex

@compileTimeOnly("enable -Ymacro-annotations")
final class LocalSpec(arg: Any) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro LocalSpec.impl
}

object LocalSpec {
  def impl(c: Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._

    def abort(msg: String) = c.abort(c.enclosingPosition, msg)

    // 1) Extract the spec-ID (string literal or cross-file HardwareSpecification reference)
    val specId: String = c.prefix.tree match {
      case q"new $_(${Literal(Constant(id: String))})" => id

      case q"new $_($expr)" =>
        // Try to resolve the symbol's fullName
        val sym = c.typecheck(expr, mode = c.TYPEmode).symbol
        val fullName = sym.fullName
        SpecIndex.idFor(fullName).getOrElse {
          c.abort(expr.pos,
            s"@LocalSpec: cannot resolve specId for ${fullName}.\n"+
            "Hint: ensure its .spec file is generated before this annotation or use @LocalSpec(\"ID\")")
        }
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
