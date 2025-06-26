package spec.core
import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.meta._

@compileTimeOnly("enable -Ymacro-annotations")
class LocalSpec(cat: SpecCategory, localId: String,
                capability: Capability = null,
                paramValues: Map[String,String] = Map.empty)
  extends StaticAnnotation {

  inline def apply(defn: Any): Any = meta {
    val q"..$mods class $tname[..$tps] ..$rest" = defn
    val src = defn.pos.input.syntax   ; val ln = defn.pos.startLine + 1
    val inject =
      q"""_root_.spec.core.SpecRegistry.add(
            _root_.spec.core.SpecRegistry.Tag(${tname.syntax}, "", $cat,
              $localId, $capability, $paramValues, $src, $ln))"""
    val template"..$stats" = rest
    val newT = template"{ ..$stats ; $inject }"
    q"..$mods class $tname[..$tps] $newT"
  }
}
