# **하드웨어 스펙 관리 프레임워크: 스펙 및 구현 가이드 (Scala 2.13.12 버전)**

릴리즈 버전: 1.0.0  
릴리즈 날짜: 2025-06-28

## **1\. 프레임워크 개요 및 목표**

이 프레임워크는 하드웨어 스펙의 정의, 관리, 검증, 그리고 RTL 구현과의 통합을 자동화하는 시스템입니다. 스펙을 코드로 관리하여 개발 과정에서 발생할 수 있는 스펙 불일치와 커뮤니케이션 문제를 최소화하는 것을 목표로 합니다.

**주요 목표:**

* **스펙 작성 및 가독성**: 스펙을 **직관적인 Builder 패턴 기반의 DSL**로 작성하여 쉽게 이해하고 관리할 수 있도록 합니다.  
* **단일 정보원**: 모든 스펙 정보는 중앙에서 관리되며, 스펙 변경 시 관련 문서와 도구가 자동으로 업데이트됩니다.  
* **자동화된 검증**: 컴파일 단계에서 스펙 유효성을 검사하고, RTL 코드와의 연결을 자동화하며, 스펙 커버리지를 추적합니다.  
* **개발 환경 통합**: IDE 기능을 활용하여 스펙 탐색 및 리팩토링을 지원합니다.  
* **스펙 현황 보고**: 빌드 시스템과 통합되어 스펙 구현 및 검증 현황을 보고서로 제공합니다.

### **1.1. 핵심 데이터 타입의 ReadWriter 정의**

프레임워크의 핵심 데이터 타입인 SpecCategory, Capability, HardwareSpecification, Tag는 upickle 라이브러리를 통해 JSON 직렬화/역직렬화가 가능하도록 implicit ReadWriter 인스턴스가 정의되어야 합니다. 이는 해당 object 내부에 macroRW를 사용하여 정의합니다.

// src/main/scala/framework/spec/HardwareSpecification.scala

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

  // SpecCategory의 implicit ReadWriter 정의  
  implicit val rw: ReadWriter\[SpecCategory\] \= macroRW  
}

// Capability 정의  
case class Capability(name: String)  
object Capability {  
  // Capability의 implicit ReadWriter 정의  
  implicit val rw: ReadWriter\[Capability\] \= macroRW  
}

/\*\*  
 \* 하드웨어 스펙의 기본 정의.  
 \* 모든 스펙은 Builder 패턴을 통해 생성된 HardwareSpecification 인스턴스입니다.  
 \*/  
abstract class HardwareSpecification {  
  val id: String  
  val category: SpecCategory  
  val description: String

  val capability: Option\[Capability\] \= None  
  val parentIds: Set\[String\] \= Set.empty  
  val metadata: Map\[String, String\] \= Map.empty  
  val status: Option\[String\] \= None  
  val relatedToIds: Set\[String\] \= Set.empty  
  val implementedBy: Option\[String\] \= None  
  val verifiedBy: Option\[String\] \= None  
  val requiredCapabilities: Set\[String\] \= Set.empty // 이 스펙이 요구하는 하위/관련 Capability 목록  
  val definitionFile: Option\[String\] \= None // 스펙이 정의된 소스 파일의 경로 (MVP에서는 자동으로 채워지지 않음)  
}

// Tag (LocalSpec 어노테이션 정보) 정의  
// @LocalSpec 매크로가 생성하는 정보를 담으며, FIRRTL Elaboration 후 최종 정보가 채워집니다.  
final case class Tag(  
  fqModule: String, // \`@LocalSpec\`가 붙은 모듈의 FQCN (FIRRTL에서 채워짐)  
  instancePath: String, // RTL 계층 구조 내의 인스턴스 경로 (FIRRTL에서 채워짐)  
  specId: String, // 태그된 스펙의 ID  
  srcFile: String, // 소스 파일 경로  
  line: Int, // 소스 라인 번호  
  column: Int // 소스 컬럼 번호  
)  
object Tag {  
  implicit val rw: ReadWriter\[Tag\] \= macroRW  
}

## **2\. 핵심 구성 요소**

프레임워크는 다음 세 가지 핵심 구성 요소로 이루어집니다.

1. **스펙 정의 파일 (Scala DSL)**: .scala 파일 내에 정의되는 스펙입니다.  
   * **HardwareSpecification**: 모든 스펙 객체의 기본 타입입니다.  
   * **Builder 패턴 DSL**: Specs.CONTRACT, Specs.FUNCTION 등 스펙 카테고리별 헬퍼를 통해 스펙을 작성합니다.  
   * **val 변수명 및 id 필드**: 각 스펙은 val 변수로 선언되며, **id 필드를 통해 고유 ID를 명시적으로 부여**합니다. 스펙 정의 시 SpecRegistry에 자동으로 등록됩니다.  
2. **RTL 구현 태깅 (@LocalSpec 어노테이션)**: RTL 코드 내에서 특정 스펙이 구현되거나 검증되는 지점을 표시하는 Scala 어노테이션입니다.  
   * **@LocalSpec(스펙\_객체\_변수명)**: 어노테이션은 인자로 해당 스펙의 **HardwareSpecification 객체 변수명**을 직접 받습니다 (예: @LocalSpec(MY\_CONTRACT\_SPEC)).  
   * **매크로 어노테이션**: 컴파일 시 @LocalSpec 매크로가 스펙 객체에서 필요한 모든 메타데이터를 추출하고 RTL 코드의 파일/라인, 모듈 경로 등의 정보를 결합하여 SpecRegistry에 **Tag 정보**를 등록합니다.  
3. **빌드 및 분석 파이프라인**: 스펙 파일과 RTL 코드로부터 스펙 정보를 추출하고, 검증하며, 보고서를 생성하는 자동화된 도구 모음입니다.  
   * **Scala 컴파일**: @LocalSpec 매크로 어노테이션 확장이 이 단계에서 이루어지며, 스펙 태그 정보가 SpecRegistry에 등록됩니다.  
   * **FIRRTL Elaboration**: RTL 인스턴스 경로와 같은 정확한 하드웨어 계층 정보가 SpecRegistry에 등록된 Tag 정보에 추가됩니다.  
   * **exportSpecIndex (SBT Task)**: SpecRegistry에 수집된 모든 HardwareSpecification 객체와 Tag 정보를 결합하여 SpecIndex.json과 ModuleIndex.json 파일을 생성합니다.  
   * **specLint (SBT Task, Optional)**: SpecIndex.json을 기반으로 스펙 누락, 사용되지 않는 스펙 등 스펙 관련 규칙을 검사하고 경고/오류를 발생시킵니다.  
   * **Verification Run**: 검증 도구들이 검증 결과 로그를 생성합니다.  
   * **reportGen**: JSON 파일과 검증 로그를 병합하여 HTML 대시보드 및 커버리지 보고서를 생성합니다.

### **2.1. 매크로 패턴 커버리지**

@LocalSpec 어노테이션은 다음 Scala 선언에 직접 적용할 수 있습니다.

|

| 대상 | 설명 | 예시 |  
| class | Chisel Module 클래스 정의 전체에 대한 스펙 (주로 CONTRACT 스펙) | @LocalSpec(MY\_MODULE\_CONTRACT) class MyModule extends Module { ... } |  
| object | 싱글톤 오브젝트에 대한 스펙 (예: 전역 설정, 인터페이스 정의) | @LocalSpec(GLOBAL\_CONFIG\_SPEC) object GlobalConfig { ... } |  
| val | 특정 값, 로직 블록, Assertion 또는 FSM 상태에 대한 스펙 | @LocalSpec(PIPELINE\_PROP) val pipeAssert \= assert(...) |  
| def | 특정 함수 또는 메서드에 대한 스펙 (Chisel def 정의) | @LocalSpec(CALC\_FUNCTION) def calculateNextState(s: UInt): UInt \= { ... } |

### **2.2. SpecPathTransform 및 FIRRTL Annotation 보강**

매크로를 통해 삽입된 @LocalSpec 정보는 초기에는 소스 파일 경로와 라인 번호만 가집니다. FIRRTL Transform 단계에서 ownerModule 및 instancePath와 같은 RTL 계층 정보를 보강합니다.

**작동 방식:**

1. **매크로**: @LocalSpec 매크로는 HardwareSpecification 객체에서 스펙 ID, 카테고리 등을 추출하고, 현재 소스 파일/라인 정보를 포함하는 Tag 객체를 SpecRegistry에 등록합니다. 이 단계에서는 ownerModule과 instancePath는 **플레이스홀더**("PLACEHOLDER\_MODULE", "PLACEHOLDER\_PATH")로 남겨둡니다.  
2. **FIRRTL Elaboration**: Chisel 코드로부터 FIRRTL 중간 표현(IR)이 생성됩니다. 이때 FIRRTL 컴파일러 플러그인(또는 커스텀 Transform)이 IR 트리를 순회하며 각 모듈 인스턴스의 실제 계층 경로를 추적합니다.  
3. **SpecPathTransform**: 이 Transform은 SpecRegistry에 등록된 Tag 정보 중 플레이스홀더로 남아있는 ownerModule과 instancePath 필드를 실제 FIRRTL IR에서 파악된 정보로 업데이트합니다. 예를 들어, Queue 클래스의 인스턴스가 top.subsys.myQueue 경로에 있다면, 해당 Tag의 instancePath를 이 경로로 채웁니다.

이 과정을 통해 SpecIndex.json 및 ModuleIndex.json에 포함될 instancePaths 정보가 정확하게 채워집니다.

## **3\. 스펙 문서 작성 가이드**

스펙 문서는 Scala 파일(.scala) 내에서 Builder 패턴 기반의 DSL을 사용합니다. 스펙이 정의될 때 자동으로 SpecRegistry에 등록되므로, 모든 스펙을 Seq로 묶어 반환할 필요가 없습니다.

### **3.1. 기본 구조**

모든 스펙은 Specs.\<CATEGORY\_KEYWORD\> 팩토리 함수로 시작하며, val 변수에 할당됩니다. 스펙 정의는 () (apply 메서드 호출)로 마무리됩니다.

// src/main/scala/your\_project/specs/MyModuleSpecs.scala

package your\_project.specs

import framework.spec.\_  
import framework.spec.Specs.\_ // CONTRACT, FUNCTION 등 팩토리 함수 임포트

object MyModuleSpecs {

  // KLASE32 CPU 프론트엔드의 최상위 계약  
  val KLASE32\_FRONTEND\_CONTRACT \= CONTRACT(  
    id \= "KLASE32\_FRONTEND\_CONTRACT", // 스펙의 고유 ID를 명시적으로 작성  
    description \= """  
      |\*\*KLASE32 CPU 프론트엔드의 최상위 계약\*\*  
      |이 스펙은 프론트엔드 모듈의 핵심 기능과 인터페이스를 정의합니다.  
      |Markdown 문법을 지원합니다.  
      |""".stripMargin.trim  
  ) hasCapability Capability("FETCH\_PREDICT\_DECODE\_QUEUE") // 메서드 체이닝으로 필드 설정  
    .withMetadata("Core\_ID", "klase32") // 계속 체이닝 가능  
    .withStatus("APPROVED") // 상태 명시  
    .requiredCapabilities(Set("AXI4\_READ", "BRANCH\_PREDICTION\_BASIC")) // 이 스펙이 요구하는 Capability  
    () // 최종적으로 apply() 호출 (괄호만 붙임)

  // AXI4-Lite 버스 인터페이스 스펙  
  val AXI4\_LITE\_INTERFACE \= INTERFACE(  
    id \= "AXI4\_LITE\_INTERFACE",  
    description \= """  
      |AXI4-Lite 버스 인터페이스 스펙입니다.  
      |이 스펙은 모든 AXI4-Lite 구현이 따라야 하는 프로토콜을 명시합니다.  
      |""".stripMargin.trim  
  ) withMetadata("Standard", "AMBA AXI4-Lite")()

  // 기타 스펙들...  
  val FETCH\_UNIT\_PIPELINE\_PROPERTY \= PROPERTY(  
    id \= "FETCH\_UNIT\_PIPELINE\_PROPERTY",  
    description \= "Fetch Unit의 파이프라인 스테이지 간 지연시간 속성."  
  ).parent("KLASE32\_FRONTEND\_CONTRACT")() // 부모 스펙 명시

}

### **3.2. 핵심 스펙 필드 및 DSL 메서드**

각 스펙 정의는 Specs.\<CATEGORY\_KEYWORD\>(id, description)으로 시작하며, SpecBuilder 객체의 메서드들을 체이닝으로 사용하여 필드를 설정합니다. 마지막에 ()를 붙여 apply 메서드를 호출합니다.

* **id \= "\<스펙\_ID\_문자열\>"**: **필수**. 스펙의 고유 식별자를 문자열로 명시합니다. val 변수명과 일치시키는 것이 좋습니다.  
* **description \= """...""".stripMargin.trim**: **필수**. 스펙의 상세 내용을 여러 줄 텍스트로 작성합니다. Markdown 문법을 사용할 수 있습니다.  
* **.parent("\<부모\_스펙\_ID\>") 또는 .parents("\<부모\_1\_ID\>", "\<부모\_2\_ID\>")**: 해당 스펙이 종속되거나 하위 속하는 \*\*부모 스펙의 ID(문자열)\*\*를 명시합니다. 다중 부모를 가질 수 있으며, specLint가 유효한 ID인지 검증합니다.  
* **.hasCapability(Capability("역할\_이름"))**: (선택 사항) 해당 스펙이 나타내는 추상적인 기능 역할을 명시합니다. framework.spec.Capability 객체를 사용합니다.  
* **.requiredCapabilities(Set("CAP\_1", "CAP\_2"))**: (선택 사항) 이 스펙(보통 CONTRACT 스펙)이 자신의 기능을 수행하기 위해 요구하는 하위/관련 Capability ID 목록을 명시합니다.  
* **.withMetadata("\<키\>", "\<값\>")**: 스펙에 대한 추가적인 구조화된 정보를 키-값 쌍으로 추가합니다. 여러 번 호출하여 여러 메타데이터를 추가할 수 있습니다.  
* **.withStatus("상태\_문자열")**: (선택 사항) 스펙의 현재 상태를 명시합니다 ("DRAFT", "APPROVED", "STABLE" 등).  
* **.relatedTo("\<관련\_스펙\_ID\>") 또는 .relatedTo("\<관련\_1\_ID\>", "\<관련\_2\_ID\>")**: (선택 사항) 직접적인 부모는 아니지만, 개념적으로 밀접하게 관련된 다른 스펙 ID들을 나열합니다.  
* **.implementedBy("코드\_경로")**: (선택 사항) 스펙이 구현된 RTL 코드의 경로(예: Scala 클래스 FQCN design.frontend.FetchUnit)를 명시합니다.  
* **.verifiedBy("검증\_경로")**: (선택 사항) 스펙이 검증된 테스트/모듈의 경로(예: Verilog 모듈 경로 verification.testbench.FetchUnitTB)를 명시합니다.

### **3.3. 스펙 카테고리**

Specs 오브젝트에서 제공하는 팩토리 함수를 사용하여 스펙 카테고리를 명시합니다.

* **Specs.CONTRACT(id, description)**: 모듈 또는 서브 모듈의 최상위 계약을 정의합니다.  
* **Specs.FUNCTION(id, description)**: 특정 기능 블록이나 동작에 대한 스펙입니다.  
* **Specs.PROPERTY(id, description)**: 시스템이 항상 만족해야 하는 불변 속성이나 제약 조건입니다.  
* **Specs.COVERAGE(id, description)**: 특정 시나리오나 상태가 검증 과정에서 도달해야 함을 정의합니다.  
* **Specs.INTERFACE(id, description)**: 모듈 간의 인터페이스나 프로토콜 정의에 사용합니다.  
* **Specs.PARAMETER(id, description)**: 디자인의 중요한 설정 파라미터에 대한 스펙입니다.  
* **Specs.RAW(id, description, prefix)**: 특정 다이어그램, 파형, 용어 정의 등 자유로운 형식의 문서적 스펙입니다. prefix는 다이어그램의 종류("Mermaid") 등을 명시할 수 있습니다.

## **4\. RTL 구현 가이드: @LocalSpec 태깅**

RTL 코드(Chisel Scala)에서 스펙이 구현되거나 검증되는 지점을 명시하기 위해 @LocalSpec 어노테이션을 사용합니다.

### **4.1. @LocalSpec 사용법**

@LocalSpec 어노테이션은 스펙의 **HardwareSpecification 객체**를 인자로 받습니다.

// src/main/scala/klase32/design/Queue.scala

package klase32.design

import chisel3.\_  
import framework.macros.LocalSpec // @LocalSpec 매크로 임포트  
import your\_project.specs.MyModuleSpecs.\_ // 스펙 객체를 임포트

// \--- CONTRACT 스펙 태그: 클래스(모듈)에 직접 적용 \---  
// 이 클래스(Module) 자체가 KLASE32\_FRONTEND\_CONTRACT 스펙을 구현함을 명시합니다.  
// 파라미터에 따라 계약이 달라진다면, 파라미터 조합별로 별도의 CONTRACT 스펙을 정의하여 사용할 수 있습니다.  
@LocalSpec(KLASE32\_FRONTEND\_CONTRACT)  
class Queue(depth: Int \= 4, w: Int \= 32\) extends Module {  
  val io \= IO(new Bundle { ... })

  // \--- PROPERTY 스펙 구현 태그 \---  
  // assert, assume 등 특정 불변 속성을 정의하는 val/def에 태그합니다.  
  @LocalSpec(FETCH\_UNIT\_PIPELINE\_PROPERTY)  
  val queuePropertyAssertion \= assert(\!(full && io.enq.ready), "Queue must not be full when enq.ready is asserted")

  // \--- FUNCTION 스펙 구현 태그 \---  
  // 특정 기능 블록이나 중요한 로직을 캡슐화하는 val/def에 태그합니다.  
  // 이 코드 블록이 AXI4\_LITE\_INTERFACE 스펙을 구현함을 명시  
  @LocalSpec(AXI4\_LITE\_INTERFACE)  
  val interfaceLogic \= {  
    // ... AXI4-Lite 인터페이스 관련 로직 ...  
  }

  // \--- PARAMETER 스펙 구현 태그 \---  
  // 모듈의 파라미터가 어떤 스펙을 따르는지 명시하는 val/def에 태그합니다.  
  @LocalSpec(AXI4\_LITE\_INTERFACE) // 예시: 이 depth 파라미터가 AXI4-Lite 인터페이스에 영향을 줌  
  val myQueueDepth \= depth  
}

### **4.2. 태깅 원칙**

* **기본 태깅 대상**: @LocalSpec는 Scala의 **class (모듈), object, val, def 선언에 태그하는 것이 기본**입니다.  
* **스펙 객체 참조**: 각 @LocalSpec 태그는 **스펙 정의 파일에 존재하는 유효한 HardwareSpecification 객체**를 참조해야 합니다. 컴파일러가 이 참조의 유효성을 검증해줍니다.  
* **적절한 위치**: 스펙이 구현되거나 검증되는 가장 의미 있는 코드 라인 또는 블록에 @LocalSpec를 태그합니다.  
* **더미 val 사용**: class 선언에 직접 태그하는 것을 제외하고, 코드 블록 내부의 특정 로직에 태그를 달기 위해서는 val dummyTag \= { ... } 또는 val dummyTag \= ()와 같은 **더미 val 선언을 플레이스홀더**로 사용해야 합니다.  
* **컨트랙트 스펙**: 각 RTL 모듈(클래스)에는 해당 모듈의 최상위 계약을 정의하는 **최소 하나의 CONTRACT 스펙**이 @LocalSpec로 태깅되어야 합니다. 이 규칙은 specLint에 의해 강제될 수 있습니다.

### **4.3. when().localtag() 와 같은 DSL의 가능성**

현재 Scala 2.13.12의 scala-reflect 매크로 한계와 Chisel의 내부 구조를 고려할 때, when().localtag()와 같은 복잡한 DSL을 **직접적으로 지원하기는 어렵습니다.** @LocalSpec 어노테이션은 val 또는 class와 같은 **선언**에만 적용될 수 있습니다. when 블록 내부의 스펙은 해당 블록을 감싸는 val에 태그하는 방식을 유지하는 것이 현실적입니다.

## **5\. 빌드 시스템 연동 (SBT 가이드)**

프레임워크는 spec-core 및 spec-plugin SBT 서브 프로젝트를 통해 빌드 시스템과 통합됩니다.

### **5.1. SBT 프로젝트 구조 및 의존성 설정**

project-root/  
├── build.sbt  
├── project/  
│   └── plugins.sbt  
├── spec-core/                // 스펙 DSL, SpecRegistry 등 핵심 타입 정의  
│   ├── src/main/scala/spec/core/SpecRegistry.scala  
│   └── ...  
├── spec-macros/              // @LocalSpec 매크로 정의 (이름 통일: specMacros)  
│   ├── src/main/scala/framework/macro/LocalSpec.scala  
│   └── ...  
├── spec-plugin/              // SBT 플러그인 정의  
│   ├── src/main/scala/spec/plugin/SpecPlugin.scala  
│   └── ...  
└── design/                   // 실제 RTL 디자인 및 스펙 정의 (사용자 프로젝트)  
    ├── src/main/scala/your\_project/specs/MyModuleSpecs.scala  
    ├── src/main/scala/your\_project/design/Queue.scala  
    └── ...

* **project/plugins.sbt**: 매크로 어노테이션을 위한 sbt-paradise 플러그인을 추가합니다.  
  addSbtPlugin("org.scalamacros" % "sbt-paradise" % "2.1.1" cross CrossVersion.full)

* **build.sbt**: 모듈 정의 및 의존성을 설정합니다. 프로젝트 이름은 SBT의 일반적인 카멜케이스(camelCase) 컨벤션을 따릅니다.  
  // \---------- global \----------  
  ThisBuild / scalaVersion := "2.13.12"  
  ThisBuild / organization := "your.company"  
  ThisBuild / version      := "0.1.0-SNAPSHOT"

  // \---------- sub-projects \----------  
  lazy val specCore \= (project in file("spec-core"))  
    .settings(  
      name := "spec-core",  
      libraryDependencies \+= "com.lihaoyi" %% "upickle" % "2.0.0", // JSON 직렬화를 위함  
      libraryDependencies \+= "org.scala-lang" % "scala-reflect" % scalaVersion.value, // 매크로를 위함  
      Compile / scalacOptions \+= "-Ymacro-annotations" // Macro Paradise 활성화  
    )

  lazy val specMacros \= (project in file("spec-macros")) // 카멜케이스로 이름 통일  
    .dependsOn(specCore) // SpecRegistry 접근을 위해 specCore에 의존  
    .settings(  
      name := "spec-macros", // SBT 내부 이름  
      libraryDependencies \+= "org.scala-lang" % "scala-reflect" % scalaVersion.value,  
      Compile / scalacOptions \+= "-Ymacro-annotations" // Macro Paradise 활성화  
    )

  lazy val specPlugin \= (project in file("spec-plugin"))  
    .dependsOn(specCore) // SpecRegistry 접근을 위해 specCore에 의존  
    .enablePlugins(SbtPlugin) // SBT 플러그인으로 활성화  
    .settings(  
      name := "spec-plugin",  
      Compile / scalacOptions \+= "-Ymacro-annotations" // Macro Paradise 활성화  
    )

  lazy val design \= (project in file("design"))  
    .dependsOn(specCore, specMacros) // 스펙 DSL 사용 및 매크로 사용을 위해 의존성 추가  
    .settings(  
      name := "design",  
      Compile / scalacOptions \+= "-Ymacro-annotations", // Macro Paradise 활성화  
      libraryDependencies \+= "edu.berkeley.cs" %% "chisel3" % "3.5.6" // Chisel 기본 의존성 (사용할 경우)  
    )

  lazy val root \= (project in file("."))  
    .aggregate(specCore, specMacros, specPlugin, design) // 모든 서브 프로젝트를 집합  
    .settings(  
      publish / skip := true // 루트 프로젝트는 배포하지 않음  
    )

### **5.2. exportSpecIndex 태스크 실행**

* 모든 스펙 정의와 RTL 태깅이 완료되면, SpecIndex.json 및 ModuleIndex.json을 생성하기 위해 다음 SBT 명령을 실행합니다.  
  sbt design/exportSpecIndex \# 'design' 프로젝트에서 실행하거나, 적절한 프로젝트 스코프 지정

* **spec-plugin/src/main/scala/spec/plugin/SpecPlugin.scala** 이 플러그인은 SpecRegistry에 수집된 HardwareSpecification 객체와 Tag 객체들을 조합하여 최종 SpecIndex.json을 생성합니다.  
  package spec.plugin

  import sbt.\_, Keys.\_  
  import spec.core.SpecRegistry  
  import upickle.default.{write \=\> uwrite} // upickle의 write 함수 임포트

  object SpecPlugin extends AutoPlugin {  
    override def requires \= plugins.JvmPlugin  
    object autoImport {  
      val exportSpecIndex \= taskKey\[Unit\]("emit SpecIndex.json")  
    }  
    import autoImport.\_  
    override lazy val projectSettings \= Seq(  
      exportSpecIndex := {  
        val log \= streams.value.log

        // 1\. SpecRegistry에 등록된 모든 HardwareSpecification 객체 가져오기  
        val allSpecs \= SpecRegistry.allSpecs.map(s \=\> s.id \-\> s).toMap

        // 2\. SpecRegistry에 등록된 모든 Tag (LocalSpec 어노테이션 정보) 가져오기  
        val allTags \= SpecRegistry.allTags

        // 3\. SpecIndex.json에 필요한 데이터 구조를 생성  
        val specIndexEntries \= allSpecs.values.map { spec \=\>  
          val relatedTags \= allTags.filter(\_.specId \== spec.id)  
          val instancePaths \= relatedTags.map { tag \=\>  
            // FIRRTL elaboration 후 채워질 필드들을 위한 플레이스홀더를 사용합니다.  
            // 현재는 매크로에서 삽입된 플레이스홀더 값이 그대로 사용됩니다.  
            Map(  
              "path" \-\> tag.instancePath,  
              "sourceFile" \-\> tag.srcFile,  
              "line" \-\> tag.line.toString,  
              "column" \-\> tag.column.toString  
            )  
          }.toList

          // verifications 필드는 MVP에서는 빈 리스트로 둡니다.  
          val verifications \= List.empty\[Map\[String, String\]\]

          Map\[String, Any\](  
            "canonicalId" \-\> spec.id,  
            "category" \-\> spec.category.toString,  
            "description" \-\> spec.description,  
            "capability" \-\> spec.capability.map(\_.name).getOrElse(null),  
            "status" \-\> spec.status.getOrElse(null),  
            "metadata" \-\> spec.metadata,  
            "ownerModule" \-\> relatedTags.headOption.map(\_.fqModule).getOrElse(null), // Tag에서 fqModule 가져옴  
            "definitionFile" \-\> spec.definitionFile.getOrElse(null), // HardwareSpecification에 definitionFile이 있다면 해당 값, 없다면 null  
            "instancePaths" \-\> instancePaths,  
            "parentIds" \-\> spec.parentIds.toList,  
            "relatedToIds" \-\> spec.relatedToIds.toList,  
            "implementedBy" \-\> spec.implementedBy.getOrElse(null),  
            "verifiedBy" \-\> spec.verifiedBy.getOrElse(null),  
            "requiredCapabilities" \-\> spec.requiredCapabilities.toList,  
            "verifications" \-\> verifications  
          )  
        }.toList

        val outDir \= (Compile/target).value // 빌드 타겟 디렉토리  
        val specIndexFile \= outDir / "SpecIndex.json"  
        IO.write(specIndexFile, uwrite(specIndexEntries, indent \= 2))  
        log.info(s"SpecIndex generated \-\> $specIndexFile")

        // ModuleIndex.json 생성 로직  
        val moduleMap \= collection.mutable.Map.empty\[String, Map\[String, Any\]\]  
        allTags.foreach { tag \=\>  
          val moduleEntry \= moduleMap.getOrElseUpdate(tag.fqModule, Map("instancePaths" \-\> List.empty, "specs" \-\> List.empty))  
          val currentInstancePaths \= moduleEntry("instancePaths").asInstanceOf\[List\[String\]\]  
          val currentSpecs \= moduleEntry("specs").asInstanceOf\[List\[String\]\]

          moduleMap.update(tag.fqModule, Map(  
            "instancePaths" \-\> (currentInstancePaths :+ tag.instancePath).distinct,  
            "specs" \-\> (currentSpecs :+ tag.specId).distinct  
          ))  
        }  
        val moduleIndexFile \= outDir / "ModuleIndex.json"  
        IO.write(moduleIndexFile, uwrite(moduleMap.toMap, indent \= 2))  
        log.info(s"ModuleIndex generated \-\> $moduleIndexFile")  
      },

      // Test 실행 시 exportSpecIndex가 먼저 실행되도록 의존성 추가  
      (Test / test) := (Test / test).dependsOn(exportSpecIndex).value  
    )  
  }

### **5.3. specLint 태스크 활용**

* 스펙 정합성 검사를 위해 specLint 태스크를 실행합니다.  
  sbt design/specLint

* **Lint 규칙 및 플래그:** specLint는 기본적으로 경고(Warn)를 발생시키지만, 특정 규칙 위반 시 빌드를 실패하도록 JVM 옵션을 설정할 수 있습니다.  
  | Rule ID | 설명 | JVM 플래그 (선택 사항) |  
  | NoContractPerModule | 각 RTL 모듈에 CONTRACT 스펙이 @LocalSpec로 태깅되지 않은 경우 | \-Dspec.fail.noContract=true |  
  | UnusedSpec | 스펙 정의 파일에 존재하지만 @LocalSpec로 한 번도 태깅되지 않은 스펙 | \-Dspec.fail.unusedSpec=true |  
  | DuplicateSpecId | 동일한 id를 가진 스펙이 여러 번 정의된 경우 | \-Dspec.fail.duplicateId=true |  
  | OrphanTag | @LocalSpec 태그가 있지만, 해당 ID를 가진 스펙 정의가 SpecRegistry에 없는 경우 | \-Dspec.fail.orphanTag=true |  
  | MissingRequiredCapability | CONTRACT 스펙이 requiredCapabilities를 명시했으나, 해당 인스턴스의 하위 모듈이 해당 Capability를 CONTRACT 스펙으로 제공하지 않는 경우 | \-Dspec.fail.missingRequiredCap=true |  
  | UntaggedTopLevel | Module이나 object와 같은 최상위 RTL 선언에 CONTRACT 스펙이 태깅되지 않은 경우 | \-Dspec.fail.untaggedTopLevel=true |

### **5.4. reportGen 태스크 활용**

* 스펙 대시보드 및 커버리지 보고서를 생성하기 위해 reportGen 태스크를 실행합니다.  
  sbt design/reportGen

  이 태스크는 SpecIndex.json, ModuleIndex.json, 그리고 검증 로그(verifications.csv)를 입력으로 받아 HTML/PDF 보고서를 생성합니다.

### **5.5. 팁: SBT 커맨드 alias**

자주 사용하는 SBT 명령에 대해 alias를 설정하여 효율성을 높일 수 있습니다. \~/.sbt/1.0/global.sbt (또는 해당 SBT 버전의 전역 설정 파일)에 다음을 추가합니다.

addCommandAlias("specReport", "design/clean; design/compile; design/exportSpecIndex; design/specLint; design/reportGen")  
addCommandAlias("lint", "design/specLint")

이제 sbt specReport 명령 한 번으로 전체 스펙 분석 및 보고서 생성 과정을 실행할 수 있습니다.

## **6\. SpecIndex.json 및 ModuleIndex.json 예시 및 필드 분석**

exportSpecIndex 태스크가 생성하는 핵심 JSON 파일들의 구조와 필드에 대해 설명합니다.

### **6.1. SpecIndex.json 스키마 (예시)**

SpecIndex.json은 시스템 내 모든 스펙 태그에 대한 플랫 카탈로그입니다.

\[  
  {  
    "canonicalId": "KLASE32\_FRONTEND\_CONTRACT",  
    "category": "CONTRACT",  
    "description": "KLASE32 CPU 프론트엔드의 최상위 계약...",  
    "capability": "FETCH\_PREDICT\_DECODE\_QUEUE",  
    "status": "APPROVED",  
    "metadata": {  
      "Core\_ID": "klase32",  
      "Revision": "H-T-FULL-2"  
    },  
    "ownerModule": "your\_project.design.Queue",  
    "definitionFile": "src/main/scala/your\_project/specs/MyModuleSpecs.scala",  
    "instancePaths": \[  
      {  
        "path": "top.subsys.myQueue",  
        "sourceFile": "src/main/scala/your\_project/design/Queue.scala",  
        "line": 15,  
        "column": 3  
      },  
      {  
        "path": "top.anotherQueue",  
        "sourceFile": "src/main/scala/your\_project/design/AnotherQueue.scala",  
        "line": 12,  
        "column": 3  
      }  
    \],  
    "parentIds": \[\],  
    "relatedToIds": \[\],  
    "implementedBy": null,  
    "verifiedBy": null,  
    "requiredCapabilities": \["AXI4\_READ", "BRANCH\_PREDICTION\_BASIC"\],  
    "verifications": \[  
      {  
        "tool": "chiseltest",  
        "suite": "QueueTester",  
        "test": "testBasicFunctionality",  
        "status": "PASS"  
      }  
    \]  
  },  
  {  
    "canonicalId": "FETCH\_UNIT\_PIPELINE\_PROPERTY",  
    "category": "PROPERTY",  
    "description": "Fetch Unit의 파이프라인 스테이지 간 지연시간 속성.",  
    "instancePaths": \[  
      {  
        "path": "top.subsys.myQueue.pipelinePropAssertion",  
        "sourceFile": "src/main/scala/your\_project/design/Queue.scala",  
        "line": 20,  
        "column": 3  
      }  
    \],  
    "parentIds": \["KLASE32\_FRONTEND\_CONTRACT"\],  
    "relatedToIds": \[\],  
    "implementedBy": null,  
    "verifiedBy": null,  
    "requiredCapabilities": \[\],  
    "verifications": \[\]  
  }  
\]

**필드 비교 및 유효성:**

| 필드명 | 현재 프레임워크의 의미 (Scala 2.13.12) |  
| canonicalId | 스펙의 고유 ID (HardwareSpecification의 id 필드) |  
| category | CONTRACT, FUNCTION, PROPERTY 등 SpecCategory enum 값 |  
| description | 스펙의 상세 설명 (HardwareSpecification의 description 필드 내용) |  
| capability | Capability 객체의 name 값 (SpecBuilder.hasCapability를 통해 설정) |  
| status | 스펙의 현재 상태 (DRAFT, APPROVED 등) (SpecBuilder.withStatus를 통해 설정) |  
| metadata | 추가적인 키-값 쌍 정보 (SpecBuilder.withMetadata를 통해 설정) |  
| ownerModule | 스펙이 태깅된 RTL 모듈의 FQCN (FIRRTL Elaboration 후 채워짐) |  
| definitionFile | 스펙 정의 파일의 경로 (스펙이 정의된 Scala 파일의 절대 경로. MVP에서는 @LocalSpec 태그가 발견된 첫 번째 소스 파일의 경로를 임시로 사용하거나 null이 될 수 있음.) |  
| instancePaths | @LocalSpec 태그가 나타나는 RTL 계층 구조 내의 모든 인스턴스 위치 (FIRRTL Elaboration 후 경로가 채워짐) |  
| parentIds | 부모 스펙 ID 목록 (SpecBuilder.parent 또는 parents를 통해 설정된 문자열 ID 집합) |  
| relatedToIds | 관련 스펙 ID 목록 (SpecBuilder.relatedTo를 통해 설정) |  
| implementedBy | 스펙 정의 시 명시된 구현 코드 경로 (SpecBuilder.implementedBy를 통해 설정) |  
| verifiedBy | 스펙 정의 시 명시된 검증 코드 경로 (SpecBuilder.verifiedBy를 통해 설정) |  
| requiredCapabilities | 이 스펙(주로 CONTRACT)이 자신의 기능을 수행하기 위해 요구하는 Capability ID 목록 (SpecBuilder.requiredCapabilities를 통해 설정) |  
| verifications | 검증 결과 목록 (외부 검증 도구에서 생성된 로그를 reportGen 단계에서 병합) |

### **6.2. ModuleIndex.json 예시**

ModuleIndex.json은 SpecIndex.json을 모듈 중심으로 그룹화한 뷰입니다. 특정 RTL 모듈이 어떤 스펙과 연관되어 있는지 쉽게 파악할 수 있습니다.

{  
  "your\_project.design.Queue": {  
    "instancePaths": \[  
      "top.subsys.myQueue",  
      "top.anotherQueue"  
    \],  
    "specs": \[  
      "KLASE32\_FRONTEND\_CONTRACT",  
      "FETCH\_UNIT\_PIPELINE\_PROPERTY",  
      "AXI4\_LITE\_INTERFACE"  
    \]  
  },  
  "your\_project.design.FetchUnit": {  
    "instancePaths": \[  
      "top.fetchUnit0"  
    \],  
    "specs": \[  
      "KLASE32\_FRONTEND\_CONTRACT",  
      "AXI4\_LITE\_INTERFACE"  
    \]  
  }  
}

