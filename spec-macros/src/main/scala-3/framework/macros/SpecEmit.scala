// spec-macros/src/main/scala-3/framework/macros/SpecEmit.scala
// Scala 3 (inline + quotes) implementation of `spec { … }`.
//
// Scala 3 has no `c.eval`, so the builder cannot be run at macro-expansion time.
// Instead this macro:
//   1. rewrites by-value relation references (`.has(intfFoo)`) to
//      `@fqn:<declarationPath>` string literals — same compiler-verified,
//      forward-reference-safe scheme as the Scala 2 version (SpecCheck resolves
//      the paths back to ids);
//   2. returns code that, at RUN TIME, builds the spec, stamps its
//      scalaDeclarationPath, and emits the `.spec` (so on Scala 3 the indices are
//      produced by running the spec objects — e.g. during elaboration — and then
//      aggregating, rather than purely at compile time).
package framework.macros

import scala.quoted.*
import framework.spec.{HardwareSpecification, MetaFile, SpecRegistry}

object SpecEmit:

  inline def spec(inline body: HardwareSpecification): HardwareSpecification =
    ${ specImpl('body) }

  def specImpl(body: Expr[HardwareSpecification])(using Quotes): Expr[HardwareSpecification] =
    import quotes.reflect.*

    // Declaration path = the fully-qualified name of the enclosing val/def.
    val fqn = Fqn.enclosingDeclPath(Symbol.spliceOwner)

    def isStableSpecRef(a: Term): Boolean =
      val stable = a match
        case _: Ident  => true
        case _: Select => true
        case _         => false
      stable && (a.tpe <:< TypeRepr.of[HardwareSpecification])
    def rewriteElem(owner: Symbol)(e: Term): Term =
      if isStableSpecRef(e) then Literal(StringConstant("@fqn:" + Fqn.normalize(e.symbol.fullName)))
      else rewriter.transformTerm(e)(owner)
    // Rewrite by-value relation arguments to `@fqn:` string literals. Varargs are
    // wrapped in `Typed(Repeated(...))`, so descend into the repeated elements.
    lazy val rewriter: TreeMap = new TreeMap:
      override def transformTerm(tree: Term)(owner: Symbol): Term = tree match
        case Apply(fun, args) =>
          val newArgs = args.map {
            case Typed(Repeated(elems, t), tpt) => Typed(Repeated(elems.map(rewriteElem(owner)), t), tpt)
            case Repeated(elems, t)             => Repeated(elems.map(rewriteElem(owner)), t)
            case other                          => rewriteElem(owner)(other)
          }
          Apply(transformTerm(fun)(owner), newArgs)
        case _ => super.transformTerm(tree)(owner)

    val rewritten =
      rewriter.transformTerm(body.asTerm)(Symbol.spliceOwner).asExprOf[HardwareSpecification]

    '{
      val _s = ${ rewritten }.copy(scalaDeclarationPath = ${ Expr(fqn) })
      MetaFile.writeSpec(_s)
      SpecRegistry.addSpec(_s)
      _s
    }
