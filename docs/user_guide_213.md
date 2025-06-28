# 하드웨어 스펙 관리 프레임워크: 스펙 및 구현 가이드 (Scala 2.13.12 버전)

-----

## 1\. 프레임워크 개요 및 목표

우리 하드웨어 개발 팀은 \*\*"스펙 파일이 곧 Micro-Architecture Specification (MAS)이자 Source of Truth"\*\*라는 철학을 바탕으로 스펙 관리 방식을 혁신합니다. 이 프레임워크는 하드웨어 스펙의 정의, 관리, 검증, 그리고 RTL 구현과의 통합을 자동화하여, 개발 과정에서 발생할 수 있는 스펙 불일치와 커뮤니케이션 오버헤드를 최소화합니다.

**주요 목표:**

  * **가독성 및 작성 편의성**: 스펙 파일을 **직관적인 Builder 패턴 기반의 DSL**로 작성하여 문서처럼 읽고 쓸 수 있게 합니다.
  * **단일 Source of Truth**: 모든 스펙 정보는 중앙 집중식으로 관리되며, 스펙 변경 시 관련 모든 문서와 도구가 자동으로 업데이트됩니다.
  * **강력한 자동화 및 검증**: 컴파일 타임에 스펙의 유효성을 검사하고, RTL 코드와의 연결을 자동화하며, 스펙 커버리지를 추적합니다.
  * **IDE 통합**: 개발 환경(IDE)의 강력한 기능을 활용하여 스펙 탐색 및 리팩토링 경험을 최적화합니다.
  * **실시간 스펙 현황 대시보드**: 빌드 시스템과 통합되어 스펙 구현 및 검증 현황을 실시간으로 제공합니다.

-----

## 2\. 핵심 구성 요소

이 프레임워크는 세 가지 핵심 구성 요소로 이루어집니다.

1.  **스펙 정의 파일 (Scala DSL)**: `.scala` 파일 내에서 정의되는 스펙 그 자체입니다.

      * **`HardwareSpecification`**: 모든 스펙 객체의 기반이 되는 추상 타입입니다.
      * **Builder 패턴 DSL**: `Specs.CONTRACT`, `Specs.FUNCTION` 등 스펙 카테고리별로 제공되는 Builder 헬퍼를 통해 스펙을 체이닝 방식으로 작성합니다.
      * **`val` 변수명 & `id` 필드**: 각 스펙은 `val` 변수로 선언되며, **`id` 필드를 통해 고유 ID를 명시적으로 부여**합니다.

2.  **RTL 구현 태깅 (`@LocalSpec` 어노테이션)**: RTL 코드 내에서 특정 스펙이 구현되거나 검증되는 지점을 표시하는 Scala 어노테이션입니다.

      * **`@LocalSpec("스펙_ID_문자열")`**: `@LocalSpec` 어노테이션은 인자로 해당 스펙의 **고유 ID 문자열**을 받습니다. 이는 스펙 정의와 RTL 태깅 간의 연결 고리 역할을 합니다.
      * **매크로 어노테이션**: 컴파일 시 `@LocalSpec` 매크로가 이 ID와 RTL 코드의 파일/라인, 모듈 경로 등의 메타데이터를 결합하여 **`FIRRTL` 어노테이션**으로 변환합니다.

3.  **빌드 및 분석 파이프라인**: 스펙 파일과 RTL 코드로부터 스펙 정보를 추출하고, 검증하며, 보고서를 생성하는 자동화된 도구 모음입니다.

      * **Scala 컴파일**: `@LocalSpec` 매크로 어노테이션 확장이 이 단계에서 이루어집니다.
      * **FIRRTL Elaboration**: RTL 인스턴스 경로와 같은 정확한 하드웨어 계층 정보가 FIRRTL 어노테이션에 추가됩니다.
      * **`exportSpecIndex` (SBT Task)**: 매크로 메타데이터와 FIRRTL 인스턴스 그래프를 결합하여 다음 JSON 파일을 생성합니다.
          * **`SpecIndex.json`**: 모든 스펙 태그에 대한 카탈로그로, 스펙의 `canonicalId`, `category`, `description`, `capability`, `ownerModule`, `definitionFile`, `instancePaths`, `parentIds`, `verifications` 등 모든 메타데이터를 포함합니다.
          * **`ModuleIndex.json`**: `SpecIndex.json`을 모듈 중심으로 그룹화한 뷰로, 각 모듈에 속한 스펙 목록과 인스턴스 경로를 보여줍니다.
      * **`specLint` (SBT Task, Optional)**: 생성된 `SpecIndex.json`을 기반으로 스펙 누락, 사용되지 않는 스펙 등 다양한 스펙 관련 규칙을 검사하고 경고/오류를 발생시킵니다.
      * **Verification Run**: `chiseltest`, JasperGold, 시뮬레이션 하네스 등 검증 도구들이 `verifications.csv` 또는 유사한 형식의 검증 결과 로그를 생성합니다.
      * **`reportGen`**: `SpecIndex.json`, `ModuleIndex.json`, 그리고 검증 로그를 병합하여 HTML 대시보드 및 커버리지 매트릭스와 같은 시각적인 보고서를 생성합니다.

-----

## 3\. 스펙 문서 작성 가이드

스펙 문서는 Scala 파일(`.scala`) 내에서 Builder 패턴 기반의 DSL 문법을 사용하여 작성합니다.

### 3.1. 기본 구조

모든 스펙은 `Specs.<CATEGORY_KEYWORD>` 팩토리 함수로 시작하며, `val` 변수에 할당됩니다. 스펙 정의는 `build` 메서드 호출로 마무리됩니다. 모든 스펙은 **`Specs.collectSpecs`** 블록 안에 포함하는 것을 권장합니다.

```scala
// <YOUR_MODULE_NAME>Specs.scala 파일 예시

package your_project.specs

import framework.spec._
import framework.spec.Specs._ // CONTRACT, FUNCTION 등 팩토리 함수 임포트

object MyModuleSpecs {

  // 모든 스펙을 collectSpecs 블록 안에 정의
  val ALL_MY_MODULE_SPECS: Seq[HardwareSpecification] = collectSpecs("My Module Specifications") {

    // ---
    // 1. 최상위 모듈 계약
    // ---
    val MY_MODULE_CONTRACT = CONTRACT( // CONTRACT 팩토리 함수 사용
      id = "MY_MODULE_CONTRACT", // **스펙의 고유 ID를 명시적으로 작성**
      description = """
        |**모듈의 최상위 계약에 대한 상세 설명**
        |
        |여러 줄로 자유롭게 작성할 수 있습니다.
        |Markdown 문법도 지원됩니다 (예: **굵게**, *기울임꼴*, `코드 블록`).
        |""".stripMargin.trim // stripMargin.trim으로 들여쓰기/공백 제거
    ) hasCapability Capability("BASIC_FUNCTIONALITY") // 체이닝으로 필드 설정
      .withMetadata("Author", "John Doe")
      .withStatus("APPROVED")
      .build // **마지막에 .build 호출 필수**

    // ... 기타 스펙 정의 ...

    // 블록의 마지막에는 정의된 모든 스펙 val들을 Seq로 묶어 반환
    Seq(
      MY_MODULE_CONTRACT,
      // ... 기타 스펙 val ...
    )
  }
}
```

### 3.2. 핵심 스펙 필드 및 DSL 메서드

각 스펙 정의는 `Specs.<CATEGORY_KEYWORD>(id, description)`으로 시작하여, 다음과 같은 Builder 메서드들을 체이닝으로 사용할 수 있습니다.

  * **`id = "<스펙_ID_문자열>"`**: **필수**. 스펙의 고유 식별자를 문자열로 명시합니다. 이 ID는 `val` 변수명과 일치하는 것이 권장됩니다 (예: `val MY_SPEC_ID = CONTRACT(id="MY_SPEC_ID", ...)`).
  * **`description = """...""".stripMargin.trim`**: **필수**. 스펙의 상세 내용을 여러 줄 텍스트로 작성합니다. Markdown 문법이 지원됩니다. `stripMargin.trim`을 사용하면 코드 내 들여쓰기가 문서에 반영되지 않고 깔끔하게 정리됩니다.
  * **`.parent("<부모_스펙_ID>")` 또는 `.parents("<부모_1_ID>", "<부모_2_ID>")`**: 해당 스펙이 종속되거나 하위 속하는 \*\*부모 스펙의 ID(문자열)\*\*를 명시합니다. 다중 부모를 가질 수 있습니다.
    *주의*: `parents` 필드에 나열된 ID는 반드시 이 프레임워크에 의해 정의된 유효한 스펙 ID여야 합니다. `specLint`에서 이를 검증합니다.
  * **`.withMetadata("<키>", "<값>")`**: 스펙에 대한 추가적인 구조화된 정보를 키-값 쌍으로 추가합니다. 여러 번 호출하여 여러 메타데이터를 추가할 수 있습니다.
    ```scala
    .withMetadata("Goal", "G-2")
    .withMetadata("Phase", "Integration")
    ```
  * **`.hasCapability(Capability("역할_이름"))`**: (선택 사항) 해당 스펙이 나타내는 추상적인 기능 역할을 명시합니다. `framework.spec.Capability` 객체를 사용합니다.
  * **`.withStatus("상태_문자열")`**: (선택 사항) 스펙의 현재 상태를 명시합니다 (`"DRAFT"`, `"PROPOSED"`, `"APPROVED"`, `"STABLE"`, `"DEPRECATED"` 등).
  * **`.relatedTo("<관련_스펙_ID>")` 또는 `.relatedTo("<관련_1_ID>", "<관련_2_ID>")`**: (선택 사항) 직접적인 부모는 아니지만, 개념적으로 밀접하게 관련된 다른 스펙 ID들을 나열합니다.
  * **`.implementedBy("코드_경로")`**: (선택 사항) 스펙이 구현된 RTL 코드의 경로(예: Scala 클래스 FQCN `design.frontend.FetchUnit`)를 명시합니다.
  * **`.verifiedBy("검증_경로")`**: (선택 사항) 스펙이 검증된 테스트/모듈의 경로(예: Verilog 모듈 경로 `verification.testbench.FetchUnitTB`)를 명시합니다.

### 3.3. 스펙 카테고리 (`CONTRACT`, `FUNCTION`, `PROPERTY`, `COVERAGE`, `INTERFACE`, `PARAMETER`, `RAW`)

`Specs` 오브젝트에서 제공하는 팩토리 함수를 사용하여 스펙 카테고리를 명시합니다.

  * **`Specs.CONTRACT(id, description)`**: 모듈 또는 서브 모듈의 최상위 계약을 정의합니다. (예: `FE_FETCH_UNIT_CONTRACT`)
  * **`Specs.FUNCTION(id, description)`**: 특정 기능 블록이나 동작에 대한 스펙입니다. (예: `FU_REQUEST_QUEUE_FUNCTION`)
  * **`Specs.PROPERTY(id, description)`**: 시스템이 항상 만족해야 하는 불변 속성이나 제약 조건입니다. (예: `FU_TIMING_FRONT_LATENCY`)
  * **`Specs.COVERAGE(id, description)`**: 특정 시나리오나 상태가 검증 과정에서 도달해야 함을 정의합니다.
  * **`Specs.INTERFACE(id, description)`**: 모듈 간의 인터페이스나 프로토콜 정의에 사용합니다. (예: `AXI4_LITE_INTERFACE`)
  * **`Specs.PARAMETER(id, description)`**: 디자인의 중요한 설정 파라미터에 대한 스펙입니다. (예: `FETCH_QUEUE_DEPTH_PARAM`)
  * **`Specs.RAW(id, description, prefix)`**: 특정 다이어그램, 파형, 용어 정의 등 자유로운 형식의 문서적 스펙입니다. `prefix`는 예를 들어 다이어그램의 종류("Mermaid") 등을 명시할 수 있습니다.

-----

## 4\. RTL 구현 가이드: `@LocalSpec` 태깅

RTL 코드(Chisel Scala)에서 스펙이 구현되거나 검증되는 지점을 명시하기 위해 `@LocalSpec` 어노테이션을 사용합니다.

### 4.1. `@LocalSpec` 사용법

`@LocalSpec` 어노테이션은 **스펙의 ID 문자열**을 유일한 인자로 받습니다.

```scala
// src/main/scala/klase32/design/Queue.scala

package klase32.design

import chisel3._
import framework.macros.LocalSpec // @LocalSpec 어노테이션 임포트
// 스펙 객체 자체는 RTL 파일에서 직접 임포트할 필요는 없지만, 스펙 ID 문자열을 알아야 합니다.

class Queue(depth: Int = 4, w: Int = 32) extends Module {
  val io = IO(new Bundle { ... })

  // --- 모듈 수준 CONTRACT 스펙 태그 ---
  // 이 Queue 모듈이 "FE_QUEUE_CONTRACT" 스펙을 구현함을 명시합니다.
  // 컨트랙트 스펙은 모듈의 파라미터에 따라 달라질 수 있으므로,
  // 파라미터 조합별로 별도의 CONTRACT 스펙 ID를 정의하고 여기에 태그할 수 있습니다.
  @LocalSpec("FE_QUEUE_CONTRACT_DEFAULT_DEPTH") // 예: "FE_QUEUE_CONTRACT_DEPTH_4"
  val queueContractTag = () // 더미 val. 어노테이션이 Scala 선언에 붙어야 하므로 사용.

  // --- Property 스펙 구현 태그 ---
  // 이 assertion 로직이 "FU_REQUEST_QUEUE_SIZE_PROP" 스펙과 관련됨을 명시
  @LocalSpec("FU_REQUEST_QUEUE_SIZE_PROP")
  assert(!(full && io.enq.ready), "Queue must not be full when enq.ready is asserted")

  // --- Function 스펙 구현 태그 ---
  val someLogicBlock = {
    // ... 로직 ...
    // 이 코드 블록이 "FU_ASSEMBLER_FSM_FUNCTION" 스펙을 구현함을 명시
    @LocalSpec("FU_ASSEMBLER_FSM_FUNCTION")
    val assemblerFsmTag = () // 역시 더미 val
    // ... FSM 로직 ...
  }

  // --- Parameter 스펙 구현 태그 ---
  // 이 depth 파라미터의 설정이 "QUEUE_DEPTH_PARAMETER" 스펙과 관련됨을 명시
  @LocalSpec("QUEUE_DEPTH_PARAMETER")
  val myQueueDepth = depth
}
```

### 4.2. 태깅 원칙

  * **매핑의 명확성**: 각 `@LocalSpec` 태그는 **스펙 정의 파일에 존재하는 유효한 ID 문자열**과 정확히 일치해야 합니다. `specLint`가 이 일치 여부를 검사합니다.
  * **적절한 위치**: 스펙이 구현되거나 검증되는 **가장 의미 있는 코드 라인 또는 블록**에 `@LocalSpec`를 태그합니다.
  * **더미 `val`**: `@LocalSpec` 어노테이션은 Scala 2.13에서 `class`, `object`, `def`, `val`과 같은 선언에만 붙을 수 있습니다. 코드 블록 내부에 태그를 달기 위해서는 `val dummyTag = ()`와 같은 **더미 `val` 선언을 플레이스홀더**로 사용해야 합니다.
  * **컨트랙트 스펙**: 각 RTL 모듈(클래스)에는 해당 모듈의 최상위 계약을 정의하는 **최소 하나의 `CONTRACT` 스펙**이 `@LocalSpec`로 태깅되어야 합니다. 이 규칙은 `specLint`에 의해 강제될 수 있습니다.

-----

## 5\. 빌드 시스템 연동 (SBT 가이드)

### 5.1. SBT 의존성 설정

  * **`project/plugins.sbt`**: 매크로 어노테이션을 위한 `sbt-paradise` 플러그인을 추가합니다.
    ```scala
    addSbtPlugin("org.scalamacros" % "sbt-paradise" % "2.1.1" cross CrossVersion.full)
    ```
  * **`project/build.sbt`**: `spec-macros` 모듈과 `scala-reflect`에 대한 의존성을 설정합니다.
    ```scala
    // spec-macros 모듈 정의
    lazy val `spec-macros` = project.in(file("spec-macros"))
      .settings(
        scalaVersion := "2.13.12",
        // 매크로는 컴파일 시점에 동작하므로 `provided` 또는 `compile` 스코프
        libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided"
      )

    // 주 프로젝트 모듈 정의
    lazy val root = project.in(file("."))
      .dependsOn(`spec-macros`) // 매크로 모듈 의존성 추가
      .settings(
        scalaVersion := "2.13.12",
        scalacOptions += "-Ymacro-annotations" // Macro Paradise 활성화
      )
    ```

### 5.2. `exportSpecIndex` 태스크 실행

  * 스펙 정의와 RTL 태깅 후, `SpecIndex.json` 및 `ModuleIndex.json`을 생성하기 위해 다음 SBT 명령을 실행합니다.
    ```bash
    sbt exportSpecIndex
    ```
    이 태스크는 컴파일된 코드 내 `@LocalSpec` 어노테이션과 FIRRTL 중간 표현에서 모든 스펙 관련 메타데이터를 추출합니다.

### 5.3. `specLint` 태스크 활용

  * 스펙 정합성 검사를 위해 `specLint` 태스크를 실행합니다.
    ```bash
    sbt specLint
    ```
  * **빌드 실패 규칙 적용**: 특정 `specLint` 규칙 위반 시 빌드를 실패하도록 JVM 옵션을 설정할 수 있습니다.
    ```bash
    sbt -Dspec.fail.noContract=true specLint
    ```
    이 옵션은 각 모듈에 `CONTRACT` 스펙이 없으면 빌드를 실패시킵니다.

### 5.4. `reportGen` 태스크 활용

  * 스펙 대시보드 및 커버리지 보고서를 생성하기 위해 `reportGen` 태스크를 실행합니다.
    ```bash
    sbt reportGen
    ```
    이 태스크는 `SpecIndex.json`, `ModuleIndex.json`, 그리고 검증 로그(`verifications.csv`)를 입력으로 받아 HTML/PDF 보고서를 생성합니다.

-----

이 가이드를 통해 Scala 2.13.12 환경에서 하드웨어 스펙 관리 프레임워크를 효과적으로 사용하고 유지보수할 수 있을 것입니다. 궁금한 점이 있다면 언제든지 문의해주세요\!