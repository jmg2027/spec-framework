package framework.macros

import scala.quoted.*

/** Shared helpers for the Scala 3 macros. */
object Fqn:

  /** Normalise a symbol `fullName` so a reference to an object member
    * (`consumer.Specs$.foo`) matches the declaration path a spec records for
    * itself (`consumer.Specs.foo`). Applied on both sides so SpecCheck resolves. */
  def normalize(s: String): String = s.replace("$.", ".").stripSuffix("$")

  /** Walk owners from `start` up to the first *real* enclosing val/def, skipping
    * the synthetic `_$macro` symbols the inline expansion introduces, and return
    * its normalised fullName (the spec's declaration path). */
  def enclosingDeclPath(using q: Quotes)(start: q.reflect.Symbol): String =
    import q.reflect.*
    var s = start
    def isReal(sym: Symbol): Boolean =
      sym.exists && (sym.isValDef || sym.isDefDef || sym.isClassDef) &&
        !sym.flags.is(Flags.Synthetic) && !sym.name.startsWith("_$")
    while s.exists && !isReal(s) do s = s.owner
    normalize(if s.exists then s.fullName else start.fullName)
