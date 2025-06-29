// -----------------------------------------------------------------------------
//  LocalSpec.scala
//  Macro annotation for tagging RTL/Chisel code with hardware specification info
//  Location: spec-macros/src/main/scala-2.13/framework/macros/LocalSpec.scala
//  Target: Scala 2.13 macro-annotation environment
// -----------------------------------------------------------------------------

package framework.macros

// Scala Reflection API imports
import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

// External types used in macro:
// - SpecRegistry: (optional) for central tag registry
// - Tag: metadata for macro-generated tag info
// - MetaFile: utility for writing JSON files at compile time
// - HardwareSpecification: for argument type checking
import _root_.framework.spec.{SpecRegistry, Tag, MetaFile, HardwareSpecification}

// -----------------------------------------------------------------------------
//  @LocalSpec
//  Macro annotation for tagging RTL/Chisel code with a hardware spec ID or object.
//  - Accepts either a String (spec ID) or a HardwareSpecification object.
//  - Emits tag metadata at compile time via MetaFile.
//  - Requires -Ymacro-annotations enabled in scalacOptions.
// -----------------------------------------------------------------------------
@compileTimeOnly("enable -Ymacro-annotations")
// (Changed): Argument type is now Any (was String), parameter renamed to specOrId.
final class LocalSpec(specOrId: Any) extends StaticAnnotation {
  // macroTransform: Entry point for macro annotation expansion.
  // - annottees: code elements being annotated (class, object, val, def, etc)
  // - delegates to LocalSpec.impl for macro logic
  def macroTransform(annottees: Any*): Any = macro LocalSpec.impl
}

// -----------------------------------------------------------------------------
//  LocalSpec macro implementation
//  - Generates Tag metadata for @LocalSpec-annotated code
//  - Writes tag info to file at compile time
//  - Does not modify the original AST
// -----------------------------------------------------------------------------
object LocalSpec {
  // impl: Main macro implementation
  // - c: macro context (compiler environment, AST access, code injection)
  // - annottees: AST nodes annotated with @LocalSpec
  // - Returns the original AST unchanged
  def impl(c: Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._ // Scala Reflection API (AST 조작, 타입 정보 접근 등)를 사용하기 위한 임포트

    // abort: Helper for fatal macro errors
    def abort(msg: String) = c.abort(c.enclosingPosition, msg)

    // 1. Extract and typecheck the @LocalSpec argument (specOrId)
    //    c.prefix.tree is the AST for the annotation itself
    val q"new $_(${specOrIdTree: c.Tree})" = c.prefix.tree

    // 2. Determine the spec ID from the argument (String literal, HardwareSpecification, or String expression)
    val specId: String =
      try {
        val typedArg = c.typecheck(specOrIdTree)
        // Prefer string literal pattern first
        typedArg match {
          case Literal(Constant(id: String)) =>
            // Direct string literal
            id
          case _ =>
            // Not a literal: check for HardwareSpecification or String expression
            val argType = typedArg.tpe
            // Debug prints (can be removed in production)
            println(s"[LocalSpec] Processing argument of type: ${argType}")
            println(s"[LocalSpec] Argument tree: ${showCode(typedArg)}")
            println(s"[LocalSpec] Argument type: ${argType.typeSymbol.name}")
            println(argType <:< c.weakTypeOf[HardwareSpecification])
            if (argType <:< c.weakTypeOf[HardwareSpecification]) {
              try {
                val specInstance = c.eval(c.Expr[HardwareSpecification](c.untypecheck(typedArg.duplicate)))
                println(s"[LocalSpec] Evaluated: $specInstance")
                specInstance.id
              } catch {
                case e: Throwable =>
                  println(s"[LocalSpec] c.eval failed: ${e.getMessage}")
                  abort(s"Failed to evaluate argument: ${e.getMessage}")
              }
            } else if (argType <:< c.weakTypeOf[String]) {
              // String expression (e.g., val s = "...")
              // Only works if evaluable at compile time
              c.eval(c.Expr[String](c.untypecheck(typedArg.duplicate)))
            } else {
              // Unsupported type
              abort(s"LocalSpec annotation expects a String literal, a HardwareSpecification object, or a String expression evaluable at compile time. Found type: ${argType}")
            }
        }
      } catch {
        case e: Exception =>
          // Exception during argument processing
          abort(s"Failed to process LocalSpec argument: ${e.getMessage}. Stack: ${e.getStackTrace.mkString("\n")}")
      }

    // 3. Extract source code position info
    val srcPath = c.enclosingPosition.source.path
    val line = c.enclosingPosition.line
    val column = c.enclosingPosition.column

    // 4. Extract fully qualified Scala declaration path
    val scalaDeclarationPath: String = c.internal.enclosingOwner.fullName + {
      annottees.head match {
        case DefDef(_, name, _, _, _, _) => "." + name.toString
        case ValDef(_, name, _, _) => "." + name.toString
        case _ => "" // class/object itself: name is already in fullName
      }
    }

    // 5. Create Tag object
    val tag = Tag(
      id                     = specId,
      scalaDeclarationPath   = scalaDeclarationPath,
      fullyQualifiedModuleName = "PLACEHOLDER_MODULE", // To be filled in FIRRTL Transform
      hardwareInstancePath   = "PLACEHOLDER_PATH",     // To be filled in FIRRTL Transform
      srcFile                = srcPath,
      line                   = line,
      column                 = column
    )

    // 6. Compile-time side effect: write .tag file via MetaFile
    MetaFile.writeTag(tag)
    c.info(c.enclosingPosition, s"[LocalSpec] wrote .tag for '${tag.id}' (Scala Decl: ${tag.scalaDeclarationPath})", force = true)

    // 7. Return the original AST unchanged
    annottees.head
  }
}
