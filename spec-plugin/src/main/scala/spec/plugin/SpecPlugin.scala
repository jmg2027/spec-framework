// spec-plugin/src/main/scala/spec/plugin/SpecPlugin.scala

package spec.plugin

import sbt._, Keys._
import _root_.framework.spec.{HardwareSpecification, SpecRegistry, Tag, SpecIndexEntry}
import upickle.default.{write => uwrite} // upickle.default.write 사용


object SpecPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  object autoImport {
    val exportSpecIndex = taskKey[Unit]("emit SpecIndex.json")
    // val specLint = taskKey[Unit]("Lint hardware specifications")
    // val reportGen = taskKey[Unit]("Generate specification reports")
  }
  import autoImport._
  override lazy val projectSettings = Seq(
    exportSpecIndex := {
      val log = streams.value.log

      // 1. SpecRegistry에 등록된 모든 HardwareSpecification 객체 가져오기
      val allSpecs = SpecRegistry.allSpecs.map(s => s.id -> s).toMap

      // 2. SpecRegistry에 등록된 모든 Tag (LocalSpec 어노테이션 정보) 가져오기
      // (수정됨): SpecRegistry.all 대신 SpecRegistry.allTags를 사용합니다.
      val allTags = SpecRegistry.allTags

      // 3. SpecIndex.json에 필요한 SpecIndexEntry 데이터 구조를 생성
      val specIndexEntries = allSpecs.values.map { spec =>
        val relatedTags = allTags.filter(_.id == spec.id)

        val instancePaths = relatedTags.map { tag =>
          Map(
            "path" -> tag.hardwareInstancePath,
            "sourceFile" -> tag.srcFile,
            "line" -> tag.line.toString,
            "column" -> tag.column.toString
          )
        }.toList

        SpecIndexEntry(
          canonicalId = spec.id,
          category = spec.category.toString,
          description = spec.description,
          capability = spec.capability.map(_.name),
          status = spec.status,
          metadata = spec.metadata.toMap,
          ownerModule = relatedTags.headOption.map(_.fullyQualifiedModuleName),
          definitionFile = spec.definitionFile,
          instancePaths = instancePaths,
          parentIds = spec.parentIds.toList,
          relatedToIds = spec.relatedToIds.toList,
          implementedBy = spec.implementedBy,
          verifiedBy = spec.verifiedBy,
          requiredCapabilities = spec.requiredCapabilities.toList
        )
      }.toList

      // 출력 디렉토리 설정 및 SpecIndex.json 파일 생성
      val outDir = (Compile / target).value
      val specIndexFile = outDir / "SpecIndex.json"
      IO.write(specIndexFile, uwrite(specIndexEntries, indent = 2))
      log.info(s"SpecIndex generated -> $specIndexFile")

      // ModuleIndex.json 생성 로직
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
      IO.write(moduleIndexFile, uwrite(moduleMapData.toMap, indent = 2))
      log.info(s"ModuleIndex generated -> $moduleIndexFile")
    },

    // Test 실행 시 exportSpecIndex가 먼저 실행되도록 의존성 추가
    (Test / test) := (Test / test).dependsOn(exportSpecIndex).value
  )
}
