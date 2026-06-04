// spec-macros/src/main/scala/framework/macros/TypedSpec.scala
// -----------------------------------------------------------------------------
//  Typed bundle specs as a macro  (the compile-time successor to Typed.bundleSpec)
// -----------------------------------------------------------------------------
//  `Typed.bundleSpec[T]` (in spec-core) works, but it relies on a runtime
//  `TypeTag`, which does not survive the `spec { … }` macro's compile-time
//  `c.eval`. That forced typed bundles to emit their `.spec` at *runtime*.
//
//  `TypedSpec.bundle[T]` removes that compromise. It extracts everything from the
//  TYPED TREES at macro-expansion time — no runtime reflection, no `c.eval` of a
//  `TypeTag`:
//
//      val bndFetchReq =
//        bundle[FetchRequest]("BND_FETCH_REQUEST", "EPM fetch request",
//          uses = "PARAM_PC_WIDTH", "PARAM_TXNID_WIDTH")(_.addr, _.txnId)
//
//   • The field NAME comes from the selector itself (`_.addr` ⇒ "addr"), so the
//     spec literally cannot name a field that does not exist — and there is no
//     string to drift from the selector.
//   • The field TYPE is read from the selector's result type in the tree.
//   • Completeness is checked against `T`'s members at compile time. The default
//     is strict — the spec must describe every public field of the bundle:
//       - `bundle`        ⇒ undeclared fields are a compile ERROR (source of truth)
//       - `bundleLenient` ⇒ undeclared fields are a WARNING (+ `undeclared-fields:`
//                           note for the checker), for bundles with intentionally
//                           internal fields
//   • The `.spec` is emitted at COMPILE time (like `spec { … }`), so no runtime
//     emission step is needed.
// -----------------------------------------------------------------------------
package framework.macros

import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import framework.spec.{HardwareSpecification, SpecCategory, MetaFile}

object TypedSpec {

  /** Typed BUNDLE spec; every public field of `T` must be declared (compile error otherwise). */
  def bundle[T](id: String, desc: String, uses: String*)(fields: (T => Any)*): HardwareSpecification =
    macro bundleImpl[T]

  /** Typed BUNDLE spec; undeclared fields are a warning, not an error. */
  def bundleLenient[T](id: String, desc: String, uses: String*)(fields: (T => Any)*): HardwareSpecification =
    macro bundleLenientImpl[T]

  def bundleImpl[T: c.WeakTypeTag](c: blackbox.Context)(
      id: c.Expr[String], desc: c.Expr[String], uses: c.Expr[String]*)(
      fields: c.Expr[T => Any]*): c.Expr[HardwareSpecification] =
    build(c)(strict = true)(id, desc, uses)(fields)

  def bundleLenientImpl[T: c.WeakTypeTag](c: blackbox.Context)(
      id: c.Expr[String], desc: c.Expr[String], uses: c.Expr[String]*)(
      fields: c.Expr[T => Any]*): c.Expr[HardwareSpecification] =
    build(c)(strict = false)(id, desc, uses)(fields)

  private def build[T: c.WeakTypeTag](c: blackbox.Context)(strict: Boolean)(
      id: c.Expr[String], desc: c.Expr[String], uses: Seq[c.Expr[String]])(
      fields: Seq[c.Expr[T => Any]]): c.Expr[HardwareSpecification] = {
    import c.universe._
    def evalStr(e: c.Expr[String]): String =
      try c.eval(c.Expr[String](c.untypecheck(e.tree.duplicate)))
      catch { case ex: Throwable => c.abort(e.tree.pos, s"bundle: argument must be a string literal: ${ex.getMessage}") }

    val idV    = evalStr(id)
    val descV  = evalStr(desc)
    val usesV  = uses.map(evalStr).toList
    require(!idV.contains(" "), s"Spec ID '$idV' must not contain spaces")

    // -- extract (name, type) from each selector tree -------------------------
    def selectOf(t: Tree): Option[(String, Type)] = t match {
      case Function(_, body) =>
        def dig(b: Tree): Option[Select] = b match {
          case s: Select          => Some(s)
          case Typed(inner, _)    => dig(inner)
          case Block(Nil, inner)  => dig(inner)
          case _                  => None
        }
        dig(body).map(s => (s.name.decodedName.toString, s.tpe))
      case _ => None
    }
    val declared: List[(String, String)] = fields.toList.map { f =>
      selectOf(f.tree).getOrElse {
        c.abort(f.tree.pos, s"each field must be a simple member selector like _.addr, got: ${showCode(f.tree)}")
      }
    }.map { case (n, t) => (n, typeLabel(t.toString)) }

    // -- completeness against T's public fields -------------------------------
    val tpe = weakTypeOf[T]
    val actual: List[String] = tpe.members.collect {
      case m: MethodSymbol if m.isGetter && m.isPublic => m.name.decodedName.toString.trim
    }.toList.distinct
    val declaredNames = declared.map(_._1).toSet
    val undeclared    = actual.filterNot(declaredNames.contains)

    val notes = scala.collection.mutable.ListBuffer("typed-bundle")
    if (undeclared.nonEmpty) {
      val msg = s"typed bundle '$idV' leaves implementation field(s) undeclared: ${undeclared.mkString(", ")}"
      if (strict) c.abort(c.enclosingPosition, msg)
      else {
        c.warning(c.enclosingPosition, msg)
        notes += ("undeclared-fields: " + undeclared.mkString(", "))
      }
    }

    // -- compile-time emission ------------------------------------------------
    val fqn = c.internal.enclosingOwner.fullName
    MetaFile.writeSpec(
      HardwareSpecification(
        id = idV, category = SpecCategory.BUNDLE, description = descV,
        uses = usesV.toSet, lists = declared, notes = notes.toList, scalaDeclarationPath = fqn))
    c.info(c.enclosingPosition, s"[TypedSpec] emitted typed bundle '$idV' (${declared.size} fields)", force = true)

    // -- runtime value reconstruction -----------------------------------------
    val pairs    = declared.map { case (n, t) => q"($n, $t)" }
    val usesLits = usesV.map(u => Literal(Constant(u)))
    val noteLits = notes.toList.map(n => Literal(Constant(n)))
    c.Expr[HardwareSpecification](q"""
      _root_.framework.spec.HardwareSpecification(
        id = $idV,
        category = _root_.framework.spec.SpecCategory.BUNDLE,
        description = $descV,
        uses = _root_.scala.collection.immutable.Set(..$usesLits),
        lists = _root_.scala.collection.immutable.List(..$pairs),
        notes = _root_.scala.collection.immutable.List(..$noteLits),
        scalaDeclarationPath = $fqn
      )
    """)
  }

  private def typeLabel(s: String): String = s.replaceFirst("^.*\\.", "")
}
