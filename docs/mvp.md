내일까지 바로 프로젝트에 삽입해서 사용하시려는 목표, 그리고 리팩토링 지원이라는 목적을 명확히 이해했습니다. 당연히 풀 피처는 아니어도, **가장 핵심적인 JSON 생성 파이프라인**을 즉시 사용할 수 있도록 **최소 기능 제품(MVP) 구현 가이드**를 제공해 드리겠습니다.

이 가이드는 `spec-core`, `spec-macros`, `spec-plugin`의 가장 필수적인 코드와 설정만 포함하며, 실제 RTL 디자인(`design` 모듈)에서 이를 어떻게 사용하는지 안내합니다.

-----

# 하드웨어 스펙 관리 프레임워크: MVP 구현 및 사용 가이드 (Scala 2.13.12)

**목표:** Scala로 작성된 스펙과 RTL 코드의 `@LocalSpec` 태그로부터 `SpecIndex.json` 및 `ModuleIndex.json` 파일을 생성합니다.

**특징:**

  * **간결한 스펙 DSL**: Builder 패턴 기반으로 스펙을 정의합니다.
  * **타입 안전한 태깅**: RTL 코드에 `@LocalSpec(스펙_객체)` 형태로 태그합니다.
  * **자동 JSON 생성**: SBT 태스크 실행 시 스펙 메타데이터가 JSON 파일로 자동 추출됩니다.

**제한사항 (MVP 버전):**

  * `ownerModule` 및 `instancePath` 필드는 **플레이스홀더 값**으로 채워집니다. (이 정보는 FIRRTL Transform이 필요한 부분이며, MVP에서는 생략합니다.)
  * `specLint` 및 `reportGen` 기능은 포함되지 않습니다.

-----

## 1\. 프로젝트 설정 (SBT)

프로젝트 루트에 `build.sbt`, `project/plugins.sbt` 파일을 생성하고, `spec-core`, `spec-macros`, `spec-plugin`, `design` 서브 프로젝트를 설정합니다.

### 1.1. `project/plugins.sbt`

```scala
// project/plugins.sbt
addSbtPlugin("org.scalamacros" % "sbt-paradise" % "2.1.1" cross CrossVersion.full)
```

### 1.2. `build.sbt`

```scala
// build.sbt
// ---------- global ----------
ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "your.company"
ThisBuild / version      := "0.1.0-SNAPSHOT"

// ---------- sub-projects ----------
lazy val specCore = (project in file("spec-core"))
  .settings(
    name := "spec-core",
    libraryDependencies += "com.lihaoyi" %% "upickle" % "2.0.0", // JSON 직렬화를 위함
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value, // 매크로를 위함
    Compile / scalacOptions += "-Ymacro-annotations" // Macro Paradise 활성화
  )

lazy val specMacros = (project in file("spec-macros")) // spec-macros 이름으로 변경
  .dependsOn(specCore) // SpecRegistry 접근을 위해 specCore에 의존
  .settings(
    name := "spec-macros",
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    Compile / scalacOptions += "-Ymacro-annotations"
  )

lazy val specPlugin = (project in file("spec-plugin"))
  .dependsOn(specCore) // SpecRegistry 접근을 위해 specCore에 의존
  .enablePlugins(SbtPlugin) // SBT 플러그인으로 활성화
  .settings(
    name := "spec-plugin",
    Compile / scalacOptions += "-Ymacro-annotations"
  )

lazy val design = (project in file("design"))
  .dependsOn(specCore, specMacros) // 스펙 DSL 사용 및 매크로 사용을 위해 의존성 추가
  .settings(
    name := "design",
    Compile / scalacOptions += "-Ymacro-annotations", // Macro Paradise 활성화
    libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.5.6" // Chisel ㅊ메 의존성 (사용할 경우)
  )

lazy val root = (project in file("."))
  .aggregate(specCore, specMacros, specPlugin, design) // 모든 서브 프로젝트를 집합
  .settings(
    publish / skip := true // 루트 프로젝트는 배포하지 않음
  )
```

-----

## 2\. `spec-core` 모듈 구현

프레임워크의 핵심 데이터 타입과 `SpecRegistry`를 정의합니다.

### 2.1. `spec-core/src/main/scala/framework/spec/HardwareSpecification.scala`

```scala
// spec-core/src/main/scala/framework/spec/HardwareSpecification.scala
package framework.spec

import upickle.default.{ReadWriter, macroRW}

// SpecCategory 정의
sealed trait SpecCategory
object SpecCategory {
  case object CONTRACT extends SpecCategory
  case object FUNCTION extends SpecCategory
  case object PROPERTY extends SpecCategory
  case object COVERAGE extends SpecCategory
  case object INTERFACE extends SpecCategory
  case object PARAMETER extends SpecCategory
  case class RAW(prefix: String) extends SpecCategory

  implicit val rw: ReadWriter[SpecCategory] = macroRW
}

// Capability 정의
case class Capability(name: String)
object Capability {
  implicit val rw: ReadWriter[Capability] = macroRW
}

/**
 * 하드웨어 스펙의 기본 정의.
 * 모든 스펙은 Builder 패턴을 통해 생성된 HardwareSpecification 인스턴스입니다.
 */
abstract class HardwareSpecification {
  val id: String
  val category: SpecCategory
  val description: String
ㅈ
  val capability: Option[Capability] = None
  val parentIds: Set[String] = Set.empty
  val metadata: Map[String, String] = Map.empty
  val status: Option[String] = None
  val relatedToIds: Set[String] = Set.empty
  val implementedBy: Option[String] = None
  val verifiedBy: Option[String] = None
  val requiredCapabilities: Set[String] = Set.empty
}

// Tag (LocalSpec 어노테이션 정보) 정의
// @LocalSpec 매크로가 생성하는 정보를 담으며, FIRRTL Elaboration 후 최종 정보가 채워집니다.
final case class Tag(
  fqModule: String, // `@LocalSpec`가 붙은 모듈의 FQCN (FIRRTL에서 채워짐)
  instancePath: String, // RTL 계층 구조 내의 인스턴스 경로 (FIRRTL에서 채워짐)
  specId: String, // 태그된 스펙의 ID
  srcFile: String, // 소스 파일 경로
  line: Int, // 소스 라인 번호
  column: Int // 소스 컬럼 번호
)
object Tag {
  implicit val rw: ReadWriter[Tag] = macroRW
}
```

### 2.2. `spec-core/src/main/scala/framework/spec/SpecBuilder.scala`

```scala
// spec-core/src/main/scala/framework/spec/SpecBuilder.scala
package framework.spec

import spec.core.SpecRegistry // SpecRegistry 임포트

class SpecBuilder(val id: String, val category: SpecCategory, val description: String) {
  private var _capability: Option[Capability] = None
  private var _parentIds: Set[String] = Set.empty
  private var _metadata: Map[String, String] = Map.empty
  private var _status: Option[String] = None
  private var _relatedToIds: Set[String] = Set.empty
  private var _implementedBy: Option[String] = None
  private var _verifiedBy: Option[String] = None
  private var _requiredCapabilities: Set[String] = Set.empty

  def hasCapability(cap: Capability): SpecBuilder = { _capability = Some(cap); this }
  def parent(parentId: String): SpecBuilder = { _parentIds += parentId; this }
  def parents(parentIds: String*): SpecBuilder = { _parentIds ++= parentIds; this }
  def withMetadata(key: String, value: String): SpecBuilder = { _metadata += (key -> value); this }
  def withStatus(s: String): SpecBuilder = { _status = Some(s); this }
  def relatedTo(relatedId: String): SpecBuilder = { _relatedToIds += relatedId; this }
  def implementedBy(path: String): SpecBuilder = { _implementedBy = Some(path); this }
  def verifiedBy(path: String): SpecBuilder = { _verifiedBy = Some(path); this }
  def requiredCapabilities(caps: Set[String]): SpecBuilder = { _requiredCapabilities = caps; this }

  // 최종 HardwareSpecification 객체를 생성하고 SpecRegistry에 등록하는 apply 메서드
  def apply(): HardwareSpecification = {
    val spec = new HardwareSpecification {
      val id = SpecBuilder.this.id
      val category = SpecBuilder.this.category
      val description = SpecBuilder.this.description
      override val capability = _capability
      override val parentIds = _parentIds
      override val metadata = _metadata
      override val status = _status
      override val relatedToIds = _relatedToIds
      override val implementedBy = _implementedBy
      override val verifiedBy = _verifiedBy
      override val requiredCapabilities = _requiredCapabilities
    }
    SpecRegistry.addSpec(spec) // 생성 시 SpecRegistry에 등록
    spec
  }
}

// 스펙 생성을 위한 팩토리 오브젝트
object Specs {
  def CONTRACT(id: String, description: String) = new SpecBuilder(id, SpecCategory.CONTRACT, description)
  def FUNCTION(id: String, description: String) = new SpecBuilder(id, SpecCategory.FUNCTION, description)
  def PROPERTY(id: String, description: String) = new SpecBuilder(id, SpecCategory.PROPERTY, description)
  def COVERAGE(id: String, description: String) = new SpecBuilder(id, SpecCategory.COVERAGE, description)
  def INTERFACE(id: String, description: String) = new SpecBuilder(id, SpecCategory.INTERFACE, description)
  def PARAMETER(id: String, description: String) = new SpecBuilder(id, SpecCategory.PARAMETER, description)
  def RAW(id: String, description: String, prefix: String) = new SpecBuilder(id, SpecCategory.RAW(prefix), description)
}
```

### 2.3. `spec-core/src/main/scala/spec/core/SpecRegistry.scala`

```scala
// spec-core/src/main/scala/spec/core/SpecRegistry.scala
package spec.core

import scala.collection.mutable
import upickle.default.{ReadWriter, macroRW}
import framework.spec.{HardwareSpecification, Tag} // HardwareSpecification, Tag 임포트

object SpecRegistry {
  // 정의된 모든 HardwareSpecification 객체들을 저장하는 버퍼
  private val specBuf = mutable.ListBuffer.empty[HardwareSpecification]
  def addSpec(spec: HardwareSpecification): Unit = specBuf += spec
  def allSpecs: List[HardwareSpecification] = specBuf.toList

  // @LocalSpec 매크로를 통해 수집된 Tag 정보들을 저장하는 버퍼
  private val tagBuf = mutable.ListBuffer.empty[Tag]
  def addTag(t: Tag): Unit = tagBuf += t
  def allTags: List[Tag] = tagBuf.toList

  // (MVP에서는 dumpTo는 SpecPlugin에서 직접 구현하므로 여기서는 사용하지 않음)
}
```

-----

## 3\. `spec-macros` 모듈 구현

`@LocalSpec` 어노테이션의 매크로 로직을 구현합니다.

### 3.1. `spec-macros/src/main/scala/framework/macro/LocalSpec.scala`

```scala
// spec-macros/src/main/scala/framework/macro/LocalSpec.scala
package framework.macros

import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context
import framework.spec.HardwareSpecification // HardwareSpecification 임포트

// 매크로 어노테이션 클래스
class LocalSpec(spec: HardwareSpecification) extends scala.annotation.StaticAnnotation {
  // macroTransform은 컴파일러가 매크로를 확장할 때 호출됩니다.
  def macroTransform(annottees: Any*): Any = macro LocalSpecMacro.impl
}

// 매크로 구현 오브젝트
object LocalSpecMacro {
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    // 어노테이션으로 전달된 HardwareSpecification 객체를 AST에서 추출
    val specExpr = c.prefix.tree match {
      case q"new LocalSpec($s)" => s // s는 HardwareSpecification 변수에 대한 AST 참조
      case _ =>
        c.abort(c.enclosingPosition, "@LocalSpec 어노테이션에 올바른 HardwareSpecification 객체를 전달해주세요.")
    }

    // 컴파일 타임에 스펙 객체의 실제 값을 평가 (!!! 핵심 !!!)
    // 이 시점에 specExpr이 가리키는 val 변수는 이미 초기화되어 있어야 합니다.
    val specValue = c.eval(c.Expr[HardwareSpecification](c.untypecheck(specExpr.duplicate)))

    // 스펙 객체에서 필요한 정보들을 추출
    val specId = specValue.id
    val srcFile = c.enclosingPosition.source.file.name
    val line = c.enclosingPosition.line
    val column = c.enclosingPosition.column

    // MVP를 위한 플레이스홀더 값 (FIRRTL Transform에서 실제 값으로 채워질 예정
    // 현재는 매크로에서 삽입된 플레이스홀더 값이 그대로 사용됩니다.
    val fqModulePlaceholder = c.enclosingClass.fullName // 가장 가까운 enclosing class/object의 FQCN
    val instancePathPlaceholder = "PLACEHOLDER_PATH"

    // Tag 객체를 생성하여 SpecRegistry에 등록하는 AST를 구성합니다.
    // 이 AST는 컴파일된 코드에 삽입됩니다.
    val tagAdditionCode =
      q"""
        _root_.spec.core.SpecRegistry.addTag(
          _root_.framework.spec.Tag(
            fqModule = ${Literal(Constant(fqModulePlaceholder))},
            instancePath = ${Literal(Constant(instancePathPlaceholder))},
            specId = ${Literal(Constant(specId))},
            srcFile = ${Literal(Constant(srcFile))},
            line = ${Literal(Constant(line))},
            column = ${Literal(Constant(column))}
          )
        )
      """

    // `@LocalSpec` 어노테이션이 붙은 대상(annottees)에 Tag 추가 코드를 삽입합니다.
    annottees.head.tree match {
      case valDef @ q"$mods val $name: $tpt = $expr" =>
        // val 정의인 경우, 원래 표현식 이전에 Tag 추가 코드를 삽입합니다.
        c.Expr(q"""
          $mods val $name: $tpt = {
            $tagAdditionCode
            $expr
          }
        """)

      case classDef @ q"$mods class $name[..$tparams] (...$paramss) extends ..$parents { ..$body }" =>
        // class 정의인 경우, 생성자 초기화 블록에 Tag 추가 코드를 삽입합니다.
        c.Expr(q"""
          $mods class $name[..$tparams] (...$paramss) extends ..$parents {
            $tagAdditionCode
            ..$body
          }
        """)

      case defDef @ q"$mods def $name[..$tparams](...$paramss): $tpt = $expr" =>
        // def 정의인 경우, 함수 본문 시작에 Tag 추가 코드를 삽입합니다.
        c.Expr(q"""
          $mods def $name[..$tparams](...$paramss): $tpt = {
            $tagAdditionCode
            $expr
          }
        """)

      case other =>
        c.abort(c.enclosingPosition, s"@LocalSpec 어노테이션은 val, def, class 선언에만 사용 가능합니다. 대상: ${showCode(other)}")
    }
  }
}
```

-----

## 4\. `spec-plugin` 모듈 구현

JSON 파일을 생성하는 SBT 태스크를 정의합니다.

### 4.1. `spec-plugin/src/main/scala/spec/plugin/SpecPlugin.scala`

```scala
// spec-plugin/src/main/scala/spec/plugin/SpecPlugin.scala
package spec.plugin

import sbt._, Keys._
import spec.core.SpecRegistry
import upickle.default.{write => uwrite} // upickle의 write 함수 임포트

object SpecPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin // JvmPlugin에 의존 (Scala 프로젝트용)
  object autoImport {
    val exportSpecIndex = taskKey[Unit]("Export SpecIndex.json and ModuleIndex.json")
  }
  import autoImport._

  override lazy val projectSettings = Seq(
    exportSpecIndex := {
      val log = streams.value.log

      // 1. SpecRegistry에 등록된 모든 HardwareSpecification 객체 가져오기
      val allSpecs = SpecRegistry.allSpecs.map(s => s.id -> s).toMap

      // 2. SpecRegistry에 등록된 모든 Tag (LocalSpec 어노테이션 정보) 가져오기
      val allTags = SpecRegistry.allTags

      // 3. SpecIndex.json에 필요한 데이터 구조를 생성
      val specIndexEntries = allSpecs.values.map { spec =>
        val relatedTags = allTags.filter(_.specId == spec.id)
        val instancePaths = relatedTags.map { tag =>
          // FIRRTL elaboration 후 채워질 필드들을 위한 플레이스홀더를 사용합니다.
          // 현재는 매크로에서 삽입된 플레이스홀더 값이 그대로 사용됩니다.
          Map(
            "path" -> tag.instancePath,
            "sourceFile" -> tag.srcFile,
            "line" -> tag.line.toString,
            "column" -> tag.column.toString,
            "ownerModule" -> tag.fqModule // Tag의 fqModule 필드를 ownerModule로 사용
          )
        }.toList

        // verifications 필드는 MVP에서는 빈 리스트로 둡니다.
        val verifications = List.empty[Map[String, String]]

        Map[String, Any](
          "canonicalId" -> spec.id,
          "category" -> spec.category.toString,
          "description" -> spec.description,
          "capability" -> spec.capability.map(_.name).getOrElse(null),
          "status" -> spec.status.getOrElse(null),
          "metadata" -> spec.metadata,
          "ownerModule" -> instancePaths.headOption.map(_("ownerModule")).getOrElse(null),
          "definitionFile" -> spec.definitionFile.getOrElse(null),
          "instancePaths" -> instancePaths,
          "parentIds" -> spec.parentIds.toList,
          "relatedToIds" -> spec.relatedToIds.toList,
          "implementedBy" -> spec.implementedBy.getOrElse(null),
          "verifiedBy" -> spec.verifiedBy.getOrElse(null),
          "requiredCapabilities" -> spec.requiredCapabilities.toList,
          "verifications" -> verifications
        )
      }.toList

      val outDir = (Compile/target).value // 빌드 타겟 디렉토리
      val specIndexFile = outDir / "SpecIndex.json"
      IO.write(specIndexFile, uwrite(specIndexEntries, indent = 2))
      log.info(s"SpecIndex generated -> $specIndexFile")

      // ModuleIndex.json 생성 로직
      val moduleMap = collection.mutable.Map.empty[String, Map[String, Any]]
      allTags.foreach { tag =>
        val moduleEntry = moduleMap.getOrElseUpdate(tag.fqModule, Map("instancePaths" -> List.empty, "specs" -> List.empty))
        val currentInstancePaths = moduleEntry("instancePaths").asInstanceOf[List[String]]
        val currentSpecs = moduleEntry("specs").asInstanceOf[List[String]]

        moduleMap.update(tag.fqModule, Map(
          "instancePaths" -> (currentInstancePaths :+ tag.instancePath).distinct,
          "specs" -> (currentSpecs :+ tag.specId).distinct
        ))
      }
      val moduleIndexFile = outDir / "ModuleIndex.json"
      IO.write(moduleIndexFile, uwrite(moduleMap.toMap, indent = 2))
      log.info(s"ModuleIndex generated -> $moduleIndexFile")
    },

    // test 태스크 실행 시 exportSpecIndex가 먼저 실행되도록 의존성 추가
    // 이렇게 하면 컴파일과 매크로 실행이 보장됩니다.
    (Test / test) := (Test / test).dependsOn(exportSpecIndex).value
  )
}
```

-----

## 5\. `design` 모듈 (사용자 프로젝트) 구현 및 실행

이제 스펙 정의와 RTL 코드에 `@LocalSpec` 태그를 달고 JSON을 생성해 봅니다.

### 5.1. `design/src/main/scala/your_project/specs/MyModuleSpecs.scala`

```scala
// design/src/main/scala/your_project/specs/MyModuleSpecs.scala
package your_project.specs

import framework.spec._
import framework.spec.Specs._

object MyModuleSpecs {

  val KLASE32_FRONTEND_CONTRACT = CONTRACT(
    id = "KLASE32_FRONTEND_CONTRACT",
    description = """
      |**KLASE32 CPU 프론트엔드의 최상위 계약**
      |이 스펙은 프론트엔드 모듈의 핵심 기능과 인터페이스를 정의합니다.
      |""".stripMargin.trim
  ) hasCapability Capability("FETCH_PREDICT_DECODE_QUEUE")
    .withMetadata("Core_ID", "klase32")
    .withStatus("APPROVED")
    .requiredCapabilities(Set("AXI4_READ", "BRANCH_PREDICTION_BASIC"))
    ()

  val AXI4_LITE_INTERFACE = INTERFACE(
    id = "AXI4_LITE_INTERFACE",
    description = """
      |AXI4-Lite 버스 인터페이스 스펙입니다.
      |""".stripMargin.trim
  ) withMetadata("Standard", "AMBA AXI4-Lite")()

  val FETCH_UNIT_PIPELINE_PROPERTY = PROPERTY(
    id = "FETCH_UNIT_PIPELINE_PROPERTY",
    description = "Fetch Unit의 파이프라인 스테이지 간 지연시간 속성."
  ).parent("KLASE32_FRONTEND_CONTRACT")()

  val EXAMPLE_FUNCTION = FUNCTION(
    id = "EXAMPLE_FUNCTION",
    description = "예시 기능 스펙."
  ).parent("KLASE32_FRONTEND_CONTRACT")()
}
```

### 5.2. `design/src/main/scala/your_project/design/Queue.scala`

```scala
// design/src/main/scala/your_project/design/Queue.scala
package your_project.design

import chisel3._
import framework.macro.LocalSpec // @LocalSpec 매크로 임포트
import your_project.specs.MyModuleSpecs._ // 스펙 객체를 임포트

// --- CONTRACT 스펙 태그: 클래스(모듈)에 직접 적용 ---
@LocalSpec(KLASE32_FRONTEND_CONTRACT)
class Queue(depth: Int = 4, w: Int = 32) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(UInt(w.W)))
    val deq = Decoupled(UInt(w.W))
  })

  // --- PROPERTY 스펙 구현 태그 ---
  @LocalSpec(FETCH_UNIT_PIPELINE_PROPERTY)
  val queuePropertyAssertion = {
    // 실제 RTL 로직 (예시)
    val full = RegInit(false.B) // 더미 로직
    assert(!full, "Queue should not be full in this state.") // 해당 속성 구현
  }

  // --- FUNCTION 스펙 구현 태그 ---
  @LocalSpec(EXAMPLE_FUNCTION)
  val someLogicBlock = {
    // ... 기능 로직 ...
    val internalCounter = RegInit(0.U(8.W)) // 더미 로직
    // ...
  }

  // --- PARAMETER 스펙 구현 태그 ---
  @LocalSpec(AXI4_LITE_INTERFACE) // AXI4_LITE_INTERFACE는 파라미터가 아니지만 예시로 사용
  val myQueueDepthParam = depth.U // RTL 파라미터가 스펙에 해당함을 태그

  // 간단한 큐 로직 (생략)
  io.enq.ready := true.B
  io.deq.valid := io.enq.valid
  io.deq.bits := io.enq.bits
}
```

### 5.3. 실행 방법

1.  **SBT 셸 시작**: 프로젝트 루트 디렉토리에서 터미널을 열고 `sbt`를 입력하여 SBT 셸에 진입합니다.
2.  **컴파일**: `design` 프로젝트를 컴파일합니다. 이 과정에서 `@LocalSpec` 매크로가 실행되고 `SpecRegistry`에 정보가 등록됩니다.
    ```
    sbt design/compile
    ```
3.  **JSON 파일 생성**: `exportSpecIndex` 태스크를 실행하여 `SpecIndex.json` 및 `ModuleIndex.json`을 생성합니다.
    ```
    sbt design/exportSpecIndex
    ```
4.  **결과 확인**: 생성된 JSON 파일은 `design/target/` 디렉토리에서 확인할 수 있습니다.
      * `design/target/SpecIndex.json`
      * `design/target/ModuleIndex.json`

이 MVP를 통해, 스펙을 Scala 코드로 작성하고 RTL 코드에 태그를 달면 자동으로 JSON 메타데이터가 생성되는 핵심 파이프라인을 즉시 경험하고 리팩토링에 활용할 수 있습니다.

-----

> **[용어 표기 일관성 안내]**
> 
> 문서 내에서 SBT 프로젝트의 lazy val 이름은 카멜케이스(`specMacros`)로, 실제 디렉토리명은 하이픈(`spec-macros`)으로 표기합니다. 예시: `lazy val specMacros = (project in file("spec-macros"))`. 설명 및 코드 예시에서는 항상 `specMacros`로 통일합니다.

> **[definitionFile 필드 안내]**
> 
> `SpecIndex.json`의 `definitionFile` 필드는 MVP에서는 “이 스펙이 태그된 첫 번째 RTL 파일”을 임시로 사용합니다. 실제 스펙 정의 파일이 아닐 수 있으니, 향후 버전에서 개선될 예정입니다.
