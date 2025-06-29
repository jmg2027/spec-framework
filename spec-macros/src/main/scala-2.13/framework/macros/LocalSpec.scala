// spec-macros/src/main/scala-2.13/framework/macros/LocalSpec.scala
// 이 파일은 spec-macros 프로젝트의 'src/main/scala-2.13/' 디렉토리에 위치하며,
// Scala 2.13 환경에서 컴파일될 매크로 구현을 담습니다.

package framework.macros

// Scala Reflection API 사용을 위한 임포트
import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

// 매크로에서 참조할 외부 타입들 임포트:
// SpecRegistry: Tag 정보를 중앙 저장소에 추가하기 위한 오브젝트 (옵션)
// Tag: SpecRegistry에 저장될 매크로 생성 정보의 데이터 타입
// MetaFile: 컴파일 시점에 JSON 파일을 쓰는 유틸리티
// HardwareSpecification: 인자 타입 검사를 위해 필요
import _root_.framework.spec.{SpecRegistry, Tag, MetaFile, HardwareSpecification}

/**
 * LocalSpec 어노테이션은 RTL (Register-Transfer Level) 코드에 하드웨어 스펙을 태그하는 데 사용됩니다.
 * 이 어노테이션은 컴파일 시점에 코드를 변환하는 매크로를 포함합니다.
 *
 * @param specOrId 이 어노테이션이 태그하는 하드웨어 스펙의 고유 ID 문자열이거나
 * [[framework.spec.HardwareSpecification]] 객체입니다.
 *
 * @compileTimeOnly 어노테이션은 이 매크로가 `scalacOptions += "-Ymacro-annotations"` 설정 없이
 * 컴파일되면 컴파일 오류를 발생시키도록 합니다.
 */
@compileTimeOnly("enable -Ymacro-annotations")
// (변경됨): 인자 타입을 String에서 Any로 변경하고 이름을 specOrId로 바꿉니다.
final class LocalSpec(specOrId: Any) extends StaticAnnotation {
  /**
   * 이 메서드는 Scala 컴파일러에 의해 호출되어 어노테이션이 적용된 AST(추상 구문 트리)를 변환합니다.
   * 실제 매크로 로직은 동반 오브젝트인 [[LocalSpec.impl]]에 정의되어 있습니다.
   *
   * `Any*` 타입은 매크로가 다양한 종류의 대상(클래스, 오브젝트, val, def 등)에 적용될 수 있음을 나타냅니다.
   */
  def macroTransform(annottees: Any*): Any = macro LocalSpec.impl
}

/**
 * LocalSpec 어노테이션 매크로의 실제 구현을 포함하는 동반 오브젝트입니다.
 * 이 매크로는 `@LocalSpec`가 붙은 Scala 코드에 [[framework.spec.Tag]] 정보를 생성하고,
 * 이를 [[framework.spec.MetaFile]]을 통해 컴파일 시점에 파일로 출력합니다.
 *
 * 이 구현은 Scala 2.13 `scala-reflect` API를 기반으로 합니다.
 */
object LocalSpec {
  /**
   * 매크로의 핵심 구현 메서드입니다.
   *
   * @param c 매크로 컨텍스트. 컴파일러 환경, AST 접근, 코드 주입 등을 위한 API를 제공합니다.
   * @param annottees `@LocalSpec` 어노테이션이 적용된 AST 노드들입니다.
   * 이 매크로는 원본 AST를 변경하지 않고 그대로 반환합니다.
   * @return 변환되지 않은 원본 AST를 반환합니다.
   */
  def impl(c: Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._ // Scala Reflection API (AST 조작, 타입 정보 접근 등)를 사용하기 위한 임포트

    /** 매크로 실행 중 치명적인 오류 발생 시 호출될 헬퍼 함수. */
    def abort(msg: String) = c.abort(c.enclosingPosition, msg)

    // 1. @LocalSpec 어노테이션의 인자 (specOrId) 추출 및 타입 검사
    // c.prefix.tree는 `@LocalSpec("MY_SPEC_ID")` 또는 `@LocalSpec(MySpecObject.MY_SPEC)` 어노테이션 자체의 AST를 나타냅니다.
    val q"new $_(${specOrIdTree: c.Tree})" = c.prefix.tree

    val specId: String =
      try {
        val typedArg = c.typecheck(specOrIdTree)

        // (수정됨): 패턴 매칭을 통해 문자열 리터럴을 가장 먼저 처리합니다.
        typedArg match {
          case Literal(Constant(id: String)) =>
            // 문자열 리터럴인 경우 직접 ID 추출
            id
          case _ =>
            // 리터럴이 아닌 경우, 타입을 확인하여 HardwareSpecification 또는 일반 String으로 처리
            val argType = typedArg.tpe
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
              // String 타입이지만 리터럴이 아닌 경우 (예: val s = "...")
              // c.eval을 통해 문자열 값을 얻어내려 시도합니다.
              // 주의: 이 경우 해당 String 표현식이 컴파일 타임에 평가 가능해야 합니다.
              c.eval(c.Expr[String](c.untypecheck(typedArg.duplicate)))
            } else {
              // 지원하지 않는 다른 타입인 경우 오류 발생
              abort(s"LocalSpec annotation expects a String literal, a HardwareSpecification object, or a String expression evaluable at compile time. Found type: ${argType}")
            }
        }
      } catch {
        case e: Exception =>
          // 인자 처리 중 예외 발생 시 자세한 메시지 출력
          abort(s"Failed to process LocalSpec argument: ${e.getMessage}. Stack: ${e.getStackTrace.mkString("\n")}")
      }

    // 2. 어노테이션이 붙은 소스 코드의 위치 정보 추출
    val srcPath = c.enclosingPosition.source.path
    val line = c.enclosingPosition.line
    val column = c.enclosingPosition.column

    // (NEW): 어노테이션이 붙은 Scala 선언의 완전한 경로를 추출합니다.
    val scalaDeclarationPath: String = c.internal.enclosingOwner.fullName + {
      // DefDef (def), ValDef (val)의 경우 enclosingOwner.fullName에 이름이 포함되지 않으므로 추가합니다.
      annottees.head match {
        case DefDef(_, name, _, _, _, _) => "." + name.toString
        case ValDef(_, name, _, _) => "." + name.toString
        case _ => "" // class 또는 object 자체인 경우 이름은 fullName에 포함됨
      }
    }

    // 3. Tag 객체 생성
    val tag = Tag(
      id                     = specId,
      scalaDeclarationPath   = scalaDeclarationPath, // 추출된 Scala 선언 경로 주입
      fullyQualifiedModuleName = "PLACEHOLDER_MODULE", // FIRRTL Transform에서 채워질 예정
      hardwareInstancePath   = "PLACEHOLDER_PATH",     // FIRRTL Transform에서 채워질 예정
      srcFile                = srcPath,
      line                   = line,
      column                 = column
    )

    // 4. 컴파일 타임 사이드 이펙트: MetaFile을 통해 .tag 파일 기록
    // 매크로가 실행되는 시점에 직접 파일을 씁니다.
    // 이 부분은 어노테이션이 붙은 원본 코드에 삽입되지 않습니다.
    MetaFile.writeTag(tag)
    c.info(c.enclosingPosition, s"[LocalSpec] wrote .tag for '${tag.id}' (Scala Decl: ${tag.scalaDeclarationPath})", force = true)

    // 5. 원본 AST를 변경하지 않고 그대로 반환
    // 어노테이션이 붙은 첫 번째 AST 노드를 그대로 반환합니다.
    annottees.head
  }
}
