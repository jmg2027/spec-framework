// spec-plugin/src/main/scala/spec/plugin/SpecPlugin.scala

package spec.plugin

import sbt._, Keys._
import _root_.framework.spec.{HardwareSpecification, SpecRegistry, Tag, SpecIndexEntry} // SpecIndexEntry 임포트
import upickle.default.{write => uwrite} // upickle.default.write 사용


object SpecPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  object autoImport {
    val exportSpecIndex = taskKey[Unit]("emit SpecIndex.json")
    // val specLint = taskKey[Unit]("Lint hardware specifications") // 향후 구현 시 활성화
    // val reportGen = taskKey[Unit]("Generate specification reports") // 향후 구현 시 활성화
  }
  import autoImport._
  override lazy val projectSettings = Seq(
    exportSpecIndex := {
      val log = streams.value.log

      // 1. SpecRegistry에 등록된 모든 HardwareSpecification 객체 가져오기
      val allSpecs = SpecRegistry.allSpecs.map(s => s.id -> s).toMap // HardwareSpecification.id 사용

      // 2. SpecRegistry에 등록된 모든 Tag (LocalSpec 어노테이션 정보) 가져오기
      val allTags = SpecRegistry.all

      // 3. SpecIndex.json에 필요한 SpecIndexEntry 데이터 구조를 생성
      // (수정됨): SpecIndexEntry 케이스 클래스를 직접 인스턴스화하고, upickle.default.write를 사용합니다.
      val specIndexEntries = allSpecs.values.map { spec =>
        val relatedTags = allTags.filter(_.id == spec.id)

        val instancePaths = relatedTags.map { tag =>
          // Tag 객체에서 필요한 정보를 추출하여 Map[String, String] 형태로 변환합니다.
          // 이 Map은 upickle이 기본적으로 직렬화할 수 있습니다.
          Map(
            "path" -> tag.hardwareInstancePath,
            "sourceFile" -> tag.srcFile,
            "line" -> tag.line.toString,
            "column" -> tag.column.toString
          )
        }.toList

        // SpecIndexEntry 객체 생성
        SpecIndexEntry(
          canonicalId = spec.id,
          category = spec.category.toString, // SpecCategory 객체를 문자열로 변환
          description = spec.description,
          capability = spec.capability.map(_.name), // Capability 객체에서 이름만 추출
          status = spec.status,
          metadata = spec.metadata.toMap, // 불변 Map으로 변환
          ownerModule = relatedTags.headOption.map(_.fullyQualifiedModuleName),
          definitionFile = spec.definitionFile,
          instancePaths = instancePaths,
          parentIds = spec.parentIds.toList, // Set을 List로 변환
          relatedToIds = spec.relatedToIds.toList, // Set을 List로 변환
          implementedBy = spec.implementedBy,
          verifiedBy = spec.verifiedBy,
          requiredCapabilities = spec.requiredCapabilities.toList // Set을 List로 변환
          // verifications = None // 향후 검증 결과 통합 시 활성화
        )
      }.toList

      // 출력 디렉토리 설정 및 SpecIndex.json 파일 생성
      // (수정됨): SpecIndexEntry 타입의 리스트를 upickle.default.write에 직접 전달합니다.
      // upickle은 SpecIndexEntry의 macroRW를 사용하여 이를 JSON으로 변환합니다.
      val outDir = (Compile / target).value
      val specIndexFile = outDir / "SpecIndex.json"
      IO.write(specIndexFile, uwrite(specIndexEntries, indent = 2))
      log.info(s"SpecIndex generated -> $specIndexFile")

      // ModuleIndex.json 생성 로직
      // (수정됨): Map[String, Any] 대신, Map[String, Map[String, List[String]]]과 같은 구체적인 타입으로 구성합니다.
      val moduleMapData = collection.mutable.Map.empty[String, Map[String, List[String]]]
      allTags.foreach { tag =>
        val currentEntry = moduleMapData.getOrElse(tag.fullyQualifiedModuleName, Map(
          "instancePaths" -> List.empty[String],
          "specs" -> List.empty[String]
        ))

        val updatedInstancePaths = (currentEntry("instancePaths") :+ tag.hardwareInstancePath).distinct
        val updatedSpecs = (currentEntry("specs") :+ tag.id).distinct

        moduleMapData.update(tag.fullyQualifiedModuleName, Map(
          "instancePaths" -> updatedInstancePaths,
          "specs" -> updatedSpecs
        ))
      }
      val moduleIndexFile = outDir / "ModuleIndex.json"
      // (수정됨): uwrite에 Map[String, Map[String, List[String]]] 타입의 Map을 직접 전달합니다.
      IO.write(moduleIndexFile, uwrite(moduleMapData.toMap, indent = 2))
      log.info(s"ModuleIndex generated -> $moduleIndexFile")
    },

    // Test 실행 시 exportSpecIndex가 먼저 실행되도록 의존성 추가
    // 이렇게 하면 테스트 실행 전에 항상 최신 스펙 인덱스가 생성됩니다.
    (Test / test) := (Test / test).dependsOn(exportSpecIndex).value
  )
}
