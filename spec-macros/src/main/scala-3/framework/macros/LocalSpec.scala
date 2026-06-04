// spec-macros/src/main/scala-3/framework/macros/LocalSpec.scala
// Scala 3 tagging.
//
// On Scala 2, RTL is anchored to a spec with the `@LocalSpec(spec)` *annotation*.
// Scala 3's macro-annotation API is still experimental and, in 3.3, crashes the
// inliner when the annotation argument is a macro-produced `val` (the spec
// objects here). So Scala 3 uses an equivalent inline *method*, `localSpec(spec)`,
// called as a statement next to the declaration being anchored. It emits the same
// `.tag` (id recorded as `@fqn:`, resolved by SpecCheck).
package framework.macros

import scala.quoted.*
import framework.spec.{HardwareSpecification, Tag, MetaFile}

/** Anchor the enclosing declaration to `spec` (the Scala 3 form of `@LocalSpec`). */
inline def localSpec(inline spec: HardwareSpecification): Unit =
  ${ localSpecImpl('spec) }

private def localSpecImpl(spec: Expr[HardwareSpecification])(using Quotes): Expr[Unit] =
  import quotes.reflect.*
  val specFqn = Fqn.normalize(spec.asTerm.underlyingArgument.symbol.fullName)
  val owner   = Fqn.enclosingDeclPath(Symbol.spliceOwner)
  val pos     = Position.ofMacroExpansion
  MetaFile.writeTag(
    Tag(
      id                       = "@fqn:" + specFqn,
      scalaDeclarationPath     = owner,
      fullyQualifiedModuleName = owner,
      hardwareInstancePath     = "",
      srcFile                  = pos.sourceFile.path,
      line                     = pos.startLine + 1,
      column                   = pos.startColumn,
      kind                     = "impl",
      expr                     = "",
    )
  )
  '{ () }
