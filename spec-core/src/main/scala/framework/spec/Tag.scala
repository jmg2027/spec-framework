// spec-core/src/main/scala/framework/spec/Tag.scala
package framework.spec

import upickle.default.{macroRW, ReadWriter}

/**
 * Tag: `@LocalSpec` 어노테이션 매크로에 의해 생성되는 메타데이터를 담는 case class입니다.
 * 이 Tag 객체는 특정 스펙이 RTL 코드의 어느 위치에 태그되었는지를 나타내는 정보를 포함합니다.
 * FIRRTL Transform 단계에서 `fullyQualifiedModuleName`과 `hardwareInstancePath`가 보강됩니다.
 *
 * (수정됨): TagIndex.json 스펙에 맞춰 `specFqn` 필드를 제거했습니다.
 * `fullyQualifiedModuleName`은 @LocalSpec가 적용된 Scala 선언의 완전한 경로를 나타냅니다.
 *
 * @param id
 * 이 Tag가 참조하는 스펙([[HardwareSpecification]])의 고유 식별자 ID입니다.
 * `HardwareSpecification.id`와 동일한 개념입니다.
 * @param fullyQualifiedModuleName
 * `@LocalSpec` 태그가 붙은 Scala 코드 요소를 감싸는 가장 가까운 Scala 클래스 또는 오브젝트의
 * 완전한 이름 (패키지 경로 포함)을 의미합니다. 이는 소스 코드 상의 위치를 식별하는 데 사용됩니다.
 * @param hardwareInstancePath
 * FIRRTL Elaboration 후 채워지는, RTL 계층 구조 내의 실제 하드웨어 인스턴스 경로입니다.
 * 슬래시(`/`) 또는 점(`.`) 등으로 구분된 모듈/인스턴스 이름의 계층적 문자열입니다
 * (예: "Top.Cpu.PipeStage").
 * @param srcFile
 * `@LocalSpec` 어노테이션이 있는 원본 Scala 소스 파일의 절대 경로입니다.
 * @param line
 * `@LocalSpec` 어노테이션이 있는 소스 파일 내 라인 번호입니다 (1-based).
 * @param column
 * `@LocalSpec` 어노테이션이 있는 소스 파일 내 컬럼 번호입니다 (0-based).
 */
final case class Tag(
  id: String,
  scalaDeclarationPath: String, // @LocalSpec가 적용된 Scala 선언의 완전한 경로
  fullyQualifiedModuleName: String, // FQN of the Scala element being tagged
  hardwareInstancePath: String,     // For FIRRTL elaboration
  srcFile: String,
  line: Int,
  column: Int
)

object Tag {
  /**
   * Tag에 대한 upickle ReadWriter를 정의합니다.
   * case class에 대해서는 macroRW가 자동으로 ReadWriter를 생성할 수 있습니다.
   */
  implicit val rw: ReadWriter[Tag] = macroRW
}
