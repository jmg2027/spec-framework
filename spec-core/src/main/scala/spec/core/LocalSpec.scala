package spec.core
import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

@compileTimeOnly("enable -Ymacro-annotations")
class LocalSpec(cat: SpecCategory, localId: String,
                capability: Capability = null,
                paramValues: Map[String,String] = Map.empty)
  extends StaticAnnotation {

  def macroTransform(annottees: Any*): Any =
    macro LocalSpec.impl
}

object LocalSpec {
  def impl(c: whitebox.Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._
    val q"new $_($catLit,$idLit,$capLit,$paramLit)" = c.prefix.tree
    def abort(msg:String)=c.abort(c.enclosingPosition,msg)

    annottees match {
      case List(cls @ q"$mods class $t[..$ps] $ct(..$paramss) extends {..$early} with ..$parents { $self => ..$stats }") =>
        val inject =
          q"""_root_.spec.core.SpecRegistry.add(
                _root_.spec.core.SpecRegistry.Tag(
                  ${t.toString}, "", $catLit, $idLit, $capLit,
                  $paramLit, ${c.enclosingPosition.source.path}, ${c.enclosingPosition.line+1}))"""
        val newBody = stats :+ inject
        val tmpl    = Template(parents, self, newBody)
        q"$mods class $t[..$ps] $ct(..$paramss) extends $tmpl"
      case _ => abort("@LocalSpec must annotate a class")
    }
  }
}
