// spec-core/src/main/scala/framework/spec/SpecIndexEntry.scala
package framework.spec

import upickle.default.{ReadWriter => RW, macroRW}

/**
 * SpecIndexEntry: SpecIndex.json 파일의 각 항목을 나타내는 강력한 타입의 케이스 클래스입니다.
 * 이 케이스 클래스는 스펙 정의와 해당 스펙이 태그된 코드 위치 정보를 구조화된 형태로 캡슐화합니다.
 * upickle의 매크로 유도 Writer/Reader를 사용하여 JSON 직렬화/역직렬화를 지원합니다.
 *
 * @param canonicalId           스펙의 고유 ID입니다.
 * @param category              스펙의 카테고리 (문자열 표현).
 * @param description           스펙에 대한 상세 설명입니다.
 * @param capability            이 스펙이 나타내는 기능 역할 (이름 문자열), 없을 경우 None.
 * @param status                스펙의 현재 상태 (예: "DRAFT", "APPROVED"), 없을 경우 None.
 * @param metadata              추가적인 메타데이터 (키-값 맵).
 * @param ownerModule           이 스펙이 태그된 주 모듈의 FQCN (첫 번째 태그 기준), 없을 경우 None.
 * @param definitionFile        스펙이 정의된 원본 소스 파일의 경로, 없을 경우 None.
 * @param instancePaths         이 스펙이 태그된 RTL 하드웨어 인스턴스 경로 및 소스 파일 정보 목록.
 * @param parentIds             부모 스펙 ID 목록.
 * @param relatedToIds          관련 스펙 ID 목록.
 * @param implementedBy         이 스펙을 구현한 코드의 경로, 없을 경우 None.
 * @param verifiedBy            이 스펙을 검증한 테스트 코드의 경로, 없을 경우 None.
 * @param requiredCapabilities  이 스펙이 요구하는 Capability ID 목록.
 */
final case class SpecIndexEntry(
  canonicalId: String,
  category: String, // SpecCategory는 toUjsonValue에서 toString()으로 변환되었으므로 String으로 받음
  description: String,
  capability: Option[String], // Capability 객체 대신 이름 문자열로 저장
  status: Option[String],
  metadata: Map[String,String],
  ownerModule: Option[String],
  definitionFile: Option[String],
  instancePaths: List[Map[String,String]], // instancePaths는 Tag에서 온 Map[String,String] 리스트로 유지
  parentIds: List[String],
  relatedToIds: List[String],
  implementedBy: Option[String],
  verifiedBy: Option[String],
  requiredCapabilities: List[String]
  // verifications: Option[List[Map[String,String]]] // 향후 검증 결과 통합 시 활성화
)
object SpecIndexEntry {
  /**
   * SpecIndexEntry 케이스 클래스에 대한 upickle ReadWriter를 자동으로 유도합니다.
   * `macroRW`는 case class의 모든 필드에 대해 적절한 Writer/Reader를 재귀적으로 찾아 직렬화를 처리합니다.
   * Option[T]는 upickle이 자동으로 None을 null로 직렬화합니다.
   */
  implicit val rw: RW[SpecIndexEntry] = macroRW
}
