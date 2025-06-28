// src/main/scala/framework/spec/SpecRegistry.scala
package framework.spec

import scala.collection.mutable.ListBuffer
import java.nio.file.{Files, Path}

/**
 * SpecRegistry: 프레임워크의 중앙 데이터 레지스트리 오브젝트입니다.
 * 모든 스펙 정의 ([[HardwareSpecification]])와 `@LocalSpec` 태그 정보 ([[Tag]])가
 * 컴파일 시점에 이곳에 수집됩니다.
 * 이 오브젝트는 디버깅 및 테스트 용도로 사용되며, 실제 하드웨어 스펙 관리는 이곳에서 이루어지지 않습니다ㅈ
 */
object SpecRegistry {
  private[spec] val specBuf: ListBuffer[HardwareSpecification] = ListBuffer.empty[HardwareSpecification]
  private[spec] val tagBuf: ListBuffer[Tag] = ListBuffer.empty[Tag]

  /**
   * 새로운 하드웨어 스펙 정의를 레지스트리에 추가합니다.
   */
  def addSpec(spec: HardwareSpecification): Unit = {
    specBuf += spec
    println(s"[DEBUG SpecRegistry] addSpec called for ID: ${spec.id}. Current total specs: ${specBuf.size}")
  }

  /**
   * `@LocalSpec` 어노테이션에 의해 생성된 태그 정보를 레지스트리에 추가합니다.
   */
  def addTag(tag: Tag): Unit = {
    tagBuf += tag
    println(s"[DEBUG SpecRegistry] addTag called for ID: ${tag.id} (Src: ${tag.srcFile}:${tag.line}). Current total tags: ${tagBuf.size}")
  }

  /**
   * 현재 레지스트리에 등록된 모든 하드웨어 스펙 정의를 반환합니다.
   */
  def allSpecs: Seq[HardwareSpecification] = {
    val currentSpecs = specBuf.toSeq
    println(s"[DEBUG SpecRegistry] allSpecs called. Returning ${currentSpecs.size} specs.")
    currentSpecs
  }

  /**
   * 현재 레지스트리에 등록된 모든 태그 정보를 반환합니다.
   */
  def allTags: Seq[Tag] = {
    val currentTags = tagBuf.toSeq
    println(s"[DEBUG SpecRegistry] allTags called. Returning ${currentTags.size} tags.")
    currentTags
  }

  /**
   * 레지스트리를 초기화합니다. (테스트 또는 빌드 클린업 용도)
   */
  def clear(): Unit = {
    specBuf.clear()
    tagBuf.clear()
    println("[DEBUG SpecRegistry] clear() called. Registry has been reset.")
  }

  // Dump all Tag objects as JSON (for plugin or SBT task use) - 이 부분은 사용되지 않는 것으로 보입니다.
  // private lazy val jsonRepr: String = upickle.default.write(all, indent = 2)
  // def dumpTo(path: Path): Unit = Files.write(path, jsonRepr.getBytes)
}