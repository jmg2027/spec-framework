// spec-macros/src/main/scala/framework/macros/LocalSpecMethod.scala
// Method form of @LocalSpec (`localSpec(spec)`).
// Emits an `impl` tag anchoring the enclosing declaration to `spec`.
package framework.macros

import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import framework.spec.{HardwareSpecification, Tag, MetaFile, SpecIndex}

object LocalSpecMethod {
  def impl(c: blackbox.Context)(spec: c.Expr[HardwareSpecification]): c.Expr[Unit] = {
    import c.universe._
    val checked = c.typecheck(spec.tree.duplicate, c.TERMmode)
    val symFqn  = Option(checked.symbol).map(_.fullName).getOrElse("")
    val id: String =
      SpecIndex.idFor(symFqn).getOrElse {
        try c.eval(c.Expr[String](c.untypecheck(q"$spec.id")))
        catch { case e: Throwable => c.abort(c.enclosingPosition, s"localSpec: cannot resolve spec id for '$symFqn': ${e.getMessage}") }
      }
    val pos   = c.enclosingPosition
    val owner = c.internal.enclosingOwner.fullName
    MetaFile.writeTag(
      Tag(
        id                       = id,
        scalaDeclarationPath     = owner,
        fullyQualifiedModuleName = owner,
        hardwareInstancePath     = "",
        srcFile                  = pos.source.path,
        line                     = pos.line,
        column                   = pos.column,
        kind                     = "impl",
        expr                     = "",
      )
    )
    c.info(pos, s"[localSpec] tag for '$id'", force = true)
    c.Expr[Unit](q"()")
  }

  /** Value form: emit the tag (anchored to the enclosing val/decl) and return decl. */
  def implValue[T](c: blackbox.Context)(spec: c.Expr[HardwareSpecification], decl: c.Expr[T]): c.Expr[T] = {
    impl(c)(spec) // emit the tag with the same id-resolution + position + owner
    decl
  }
}
