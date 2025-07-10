// -----------------------------------------------------------------------------
//  @LocalSpec  – compile-time macro using a HardwareSpecification argument
//  Location: spec-macros/src/main/scala-2.13/framework/macros/LocalSpec.scala
// -----------------------------------------------------------------------------

package framework.macros

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox
import framework.spec.{Tag, MetaFile, HardwareSpecification, SpecIndex}

@compileTimeOnly("enable -Ymacro-annotations")
final class LocalSpec(spec: HardwareSpecification) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro LocalSpec.impl
}

object LocalSpec {
  def impl(c: whitebox.Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._

    def abort(msg: String) = c.abort(c.enclosingPosition, msg)

    // Extract specId from annotation argument (HardwareSpecification reference)
    val specId: String = c.prefix.tree match {
      case q"new $_($expr)" =>
        val tpe     = c.typecheck(expr, c.TERMmode).tpe
        val specTpe = typeOf[HardwareSpecification]
        if (!(tpe <:< specTpe))
          abort(s"@LocalSpec expects a HardwareSpecification, got: $tpe")
        val sym      = c.typecheck(expr, c.TERMmode).symbol
        val fullName = sym.fullName
        SpecIndex.idFor(fullName).getOrElse {
          // Fallback: evaluate the spec object to obtain its id
          try {
            c.eval(c.Expr[String](q"$expr.id"))
          } catch {
            case e: Throwable =>
              abort(s"@LocalSpec: cannot resolve specId for $fullName and evaluation failed: ${e.getMessage}")
          }
        }
      case _ => abort("Invalid @LocalSpec argument")
    }

    val pos = c.enclosingPosition
    val scalaDeclPath = c.internal.enclosingOwner.fullName + {
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
      scalaDeclarationPath    = scalaDeclPath,
      fullyQualifiedModuleName= c.internal.enclosingOwner.fullName,
      hardwareInstancePath    = "",
      srcFile                 = pos.source.path,
      line                    = pos.line,
      column                  = pos.column
    )

    MetaFile.writeTag(tag)
    c.info(pos, s"[LocalSpec] emitted .tag for '${tag.id}'", force = true)

    annottees.head
  }
}
