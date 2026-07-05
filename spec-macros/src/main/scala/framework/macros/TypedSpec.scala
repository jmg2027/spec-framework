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

  /**
   * Typed PARAMETER spec, emitted at COMPILE time (build-time pure).
   *
   * The field name and type come from the selector (`_.dataBytes`) — a rename is
   * a compile error — and the `default` is a compile-time literal. The `.spec` is
   * written during `compile` (like `bundle` / `spec { … }`), so no run /
   * elaboration is needed to materialise it. This is the canonical form for a
   * production Chisel/ASIC build, where the spec graph must be a pure build-time
   * artefact (the CI gate runs on `compile`, not on a Chisel run).
   *
   * Trade-off vs [[paramSpec]]: the default is restated here as a literal rather
   * than read from a live config instance. The selector still binds the name/type
   * to the config field (rename-safe); only the default value is duplicated. If
   * you would rather single-source the default from the config and can afford a
   * runtime emission pass, use [[paramSpec]].
   */
  def param[T](id: String, desc: String, default: Any)(sel: T => Any): HardwareSpecification =
    macro paramCTImpl[T]

  /**
   * Typed PARAMETER spec bound to a config field, emitted at RUN time.
   *
   * The field name and type come from the selector (`_.dataBytes`) — a rename is
   * a compile error — and the `default` is read from `cfg` at run time, so the
   * spec and the config case class can no longer drift (single source of truth
   * for parameter defaults). The cost is that the `.spec` is emitted at runtime,
   * so materialising it needs a run (`Elaborate`/a test), not just `compile` —
   * unlike [[param]], which is build-time pure. Prefer [[param]] when the build
   * must stay run-free; use this when single-sourcing the default matters more.
   */
  def paramSpec[T](id: String, desc: String, cfg: T)(sel: T => Any): HardwareSpecification =
    macro paramImpl[T]

  /** Compile-time PARAMETER spec: name/type from the selector tree, default a
    * compile-time literal, `.spec` written at macro expansion (build-time pure). */
  def paramCTImpl[T: c.WeakTypeTag](c: blackbox.Context)(
      id: c.Expr[String], desc: c.Expr[String], default: c.Expr[Any])(
      sel: c.Expr[T => Any]): c.Expr[HardwareSpecification] = {
    import c.universe._
    // Read compile-time literals straight from the trees. We deliberately avoid
    // c.eval here: it spins up a sub-compiler that opens the whole classpath, and
    // doing that 3× per param (id, desc, default) exhausts the file-descriptor
    // budget on a large Chisel classpath. The args are literals, so the tree is a
    // Literal(Constant(_)); only fall back to c.eval for the rare non-literal.
    def litConst(t: c.Tree): Option[Any] = t match {
      case Literal(Constant(v)) => Some(v)
      case Typed(e, _)          => litConst(e)
      case Block(_, e)          => litConst(e)
      case Apply(_, List(e))    => litConst(e) // boxing wrappers e.g. Int→Any
      case _                    => None
    }
    def litStr(e: c.Tree, what: String): String = litConst(e) match {
      case Some(s) => String.valueOf(s)
      case None    =>
        try c.eval(c.Expr[String](c.untypecheck(e.duplicate)))
        catch { case ex: Throwable =>
          c.abort(e.pos, s"param: $what must be a compile-time constant: ${ex.getMessage}. " +
            "Use paramSpec[T](id, desc, cfg)(sel) for a default read from a runtime config.") }
    }

    val idV   = litStr(id.tree, "id")
    val descV = litStr(desc.tree, "desc")
    require(!idV.contains(" "), s"Spec ID '$idV' must not contain spaces")

    val (fieldName, fieldType) = sel.tree match {
      case Function(_, body) =>
        def dig(t: Tree): Option[Select] = t match {
          case s: Select   => Some(s)
          case Typed(e, _) => dig(e)
          case Block(_, e) => dig(e)
          case _           => None
        }
        dig(body).map(s => (s.name.decodedName.toString, typeLabel(s.tpe.toString)))
          .getOrElse(c.abort(sel.tree.pos, "param selector must be a simple field access like _.dataBytes"))
      case _ => c.abort(sel.tree.pos, "param selector must be a function literal")
    }
    val defaultV = litStr(default.tree, "default")
    val fqn = c.internal.enclosingOwner.fullName

    val entries = List("name" -> fieldName, "type" -> fieldType, "default" -> defaultV)
    MetaFile.writeSpec(
      HardwareSpecification(
        id = idV, category = SpecCategory.PARAMETER, description = descV,
        lists = entries,
        scalaDeclarationPath = fqn))
    c.info(c.enclosingPosition, s"[TypedSpec] emitted PARAMETER '$idV' (default=$defaultV)", force = true)

    val entryLits = entries.map { case (k, v) => q"($k, $v)" }
    c.Expr[HardwareSpecification](q"""
      _root_.framework.spec.HardwareSpecification(
        id = $idV,
        category = _root_.framework.spec.SpecCategory.PARAMETER,
        description = $descV,
        lists = _root_.scala.collection.immutable.List(..$entryLits),
        scalaDeclarationPath = $fqn
      )
    """)
  }

  def paramImpl[T](c: blackbox.Context)(
      id: c.Expr[String], desc: c.Expr[String], cfg: c.Expr[T])(
      sel: c.Expr[T => Any]): c.Expr[HardwareSpecification] = {
    import c.universe._
    val fieldName = sel.tree match {
      case Function(_, body) =>
        def dig(t: Tree): Option[Select] = t match {
          case s: Select       => Some(s)
          case Typed(e, _)     => dig(e)
          case Block(_, e)     => dig(e)
          case _               => None
        }
        dig(body).map(_.name.decodedName.toString)
          .getOrElse(c.abort(sel.tree.pos, "paramSpec selector must be a simple field access like _.dataBytes"))
      case _ => c.abort(sel.tree.pos, "paramSpec selector must be a function literal")
    }
    val fieldType = sel.tree match {
      case Function(_, body) => typeLabel(body.tpe.toString)
      case _                 => "?"
    }
    val fqn = c.internal.enclosingOwner.fullName
    c.Expr[HardwareSpecification](q"""{
      val _s = _root_.framework.spec.Spec.PARAMETER($id).desc($desc)
        .entry("name", $fieldName).entry("type", $fieldType)
        .entry("default", _root_.java.lang.String.valueOf($sel($cfg)))
        .build().copy(scalaDeclarationPath = $fqn)
      _root_.framework.spec.MetaFile.writeSpec(_s)
      _s
    }""")
  }

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

    // -- completeness against T's *own declared* public fields ----------------
    // Use declarations (not inherited members) so base classes — e.g. chisel3's
    // Bundle/Record/Data — do not count as undeclared fields.
    val tpe = weakTypeOf[T]
    val actual: List[String] = tpe.decls.collect {
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
