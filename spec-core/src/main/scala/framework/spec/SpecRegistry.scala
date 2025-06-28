// src/main/scala/framework/spec/SpecRegistry.scala (hw_spec_guide_2_13_12 immersive에서 발췌)
package framework.spec

import scala.collection.mutable.ListBuffer
// HardwareSpecification과 Tag는 이제 동일 패키지에 있으므로 임포트 불필요

/**
 * SpecRegistry: 프레임워크의 중앙 데이터 레지스트리 오브젝트입니다.
 * 모든 스펙 정의 ([[HardwareSpecification]])와 `@LocalSpec` 태그 정보 ([[Tag]])가
 * 컴파일 시점에 이곳에 수집됩니다.
 */
object SpecRegistry {
  private[spec] val specBuf: ListBuffer[HardwareSpecification] = ListBuffer.empty[HardwareSpecification]
  private[spec] val tagBuf: ListBuffer[Tag] = ListBuffer.empty[Tag]

  /**
   * 새로운 하드웨어 스펙 정의를 레지스트리에 추가합니다.
   */
  def addSpec(spec: HardwareSpecification): Unit = specBuf += spec

  /**
   * `@LocalSpec` 어노테이션에 의해 생성된 태그 정보를 레지스트리에 추가합니다.
   */
  def addTag(tag: Tag): Unit = tagBuf += tag

  /**
   * 현재 레지스트리에 등록된 모든 하드웨어 스펙 정의를 반환합니다.
   */
  def allSpecs: Seq[HardwareSpecification] = specBuf.toSeq // Seq로 변경되어 있었음

  /**
   * 현재 레지스트리에 등록된 모든 태그 정보를 반환합니다.
   */
  def allTags: Seq[Tag] = tagBuf.toSeq // <-- 원래 'allTags' 였습니다.

  /**
   * 레지스트리를 초기화합니다. (테스트 또는 빌드 클린업 용도)
   */
  def clear(): Unit = {
    specBuf.clear()
    tagBuf.clear()
  }
}
