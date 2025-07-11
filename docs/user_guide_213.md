# **하드웨어 스펙 관리 프레임워크: 스펙 및 구현 가이드 (Scala 2.13.12 버전)**

릴리즈 버전: 1.0.8 (대규모 업데이트 \- DSL, @LocalSpec, Tag 경로 정보)  
릴리즈 날짜: 2025-06-29

## **1\. 프레임워크 개요 및 목표**

이 프레임워크는 하드웨어 스펙의 정의, 관리, 검증, 그리고 RTL 구현과의 통합을 자동화하는 시스템입니다. 스펙을 코드로 관리하여 개발 과정에서 발생할 수 있는 스펙 불일치와 커뮤니케이션 문제를 최소화하는 것을 목표로 합니다.

**주요 목표:**

* **스펙 작성 및 가독성**: 스펙을 **순수 함수형 Staged Builder 기반의 DSL**로 작성하여 쉽게 이해하고 관리할 수 있도록 합니다.  
* **단일 정보원**: 모든 스펙 정보는 중앙에서 관리되며, 스펙 변경 시 관련 문서와 도구가 자동으로 업데이트됩니다.  
* **자동화된 검증**: 컴파일 단계에서 스펙 유효성을 검사하고, RTL 코드와의 연결을 자동화하며, 스펙 커버리지를 추적합니다.  
* **스펙 현황 보고**: 빌드 시스템과 통합되어 스펙 구현 및 검증 현황을 보고서로 제공합니다.

### **1.1. 핵심 데이터 타입의 ReadWriter 정의**

프레임워크의 핵심 데이터 타입인 SpecCategory, Capability, HardwareSpecification, Tag는 upickle 라이브러리를 통해 JSON 직렬화/역직렬화가 가능하도록 implicit ReadWriter 인스턴스가 정의되어야 합니다. 이들은 framework.spec 패키지 내에 정의됩니다.

// src/main/scala/framework/spec/HardwareSpecification.scala  
package framework.spec

/\*\*  
 \* HardwareSpecification: 하드웨어 스펙의 구체적인 정의를 나타내는 final case class입니다.  
 \* 모든 스펙 정의는 이 클래스의 인스턴스로 표현됩니다.  
 \*/  
final case class HardwareSpecification(  
  id: String,  
  category: SpecCategory,  
  description: String,  
  capability: Option\[Capability\],  
  status: Option\[String\],  
  metadata: Map\[String, String\],  
  parentIds: Set\[String\],  
  relatedToIds: Set\[String\],  
  implementedBy: Option\[String\],  
  verifiedBy: Option\[String\],  
  requiredCapabilities: Set\[String\],  
  definitionFile: Option\[String\],  
  lists: List[(String, String)]
)  
object HardwareSpecification {  
  implicit val rw: ReadWriter\[HardwareSpecification\] \= macroRW  
}

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

  implicit val rw: ReadWriter\[SpecCategory\] \= ReadWriter\[SpecCategory\](  
    Writer\[SpecCategory\] {  
      case CONTRACT \=\> ujson.Str("CONTRACT")  
      case FUNCTION \=\> ujson.Str("FUNCTION")  
      case PROPERTY \=\> ujson.Str("PROPERTY")  
      case COVERAGE \=\> ujson.Str("COVERAGE")  
      case INTERFACE \=\> ujson.Str("INTERFACE")  
      case PARAMETER \=\> ujson.Str("PARAMETER")  
      case RAW(prefix) \=\> ujson.Str(s"RAW:$prefix")  
    },  
    Reader\[SpecCategory\] {  
      case ujson.Str("CONTRACT") \=\> CONTRACT  
      case ujson.Str("FUNCTION") \=\> FUNCTION  
      case ujson.Str("PROPERTY") \=\> PROPERTY  
      case ujson.Str("COVERAGE") \=\> COVERAGE  
      case ujson.Str("INTERFACE") \=\> INTERFACE  
      case ujson.Str("PARAMETER") \=\> PARAMETER  
      case s if s.startsWith("RAW:") \=\> RAW(s.substring(4))  
      case value \=\> throw new IllegalArgumentException(s"Unknown SpecCategory string: $value")  
    }  
  )  
}

// Capability 정의  
final case class Capability(name: String)  
object Capability {  
  implicit val rw: ReadWriter\[Capability\] \= macroRW  
}  
\`\`\`scala  
// src/main/scala/framework/spec/Tag.scala  
package framework.spec

import upickle.default.{ReadWriter, macroRW}

/\*\*  
 \* Tag: \`@LocalSpec\` 어노테이션 매크로에 의해 생성되는 메타데이터를 담는 case class입니다.  
 \* 이 Tag 객체는 특정 스펙이 RTL 코드의 어느 위치에 태그되었는지를 나타내는 정보를 포함합니다.  
 \* FIRRTL Transform 단계에서 \`fullyQualifiedModuleName\`과 \`hardwareInstancePath\`가 보강됩니다.  
 \*/  
final case class Tag(  
  id: String,  
  scalaDeclarationPath: String, // (추가됨): @LocalSpec가 적용된 Scala 선언의 완전한 경로  
  fullyQualifiedModuleName: String,  
  hardwareInstancePath: String,  
  srcFile: String,  
  line: Int,  
  column: Int  
)  
object Tag {  
  implicit val rw: ReadWriter\[Tag\] \= macroRW  
}  
\`\`\`scala  
// src/main/scala/framework/spec/Spec.scala (SpecBuilder 및 Specs 객체를 대체)  
package framework.spec

// 순수 함수형 DSL의 전체 코드는 문서 하단 '스펙 정의 방식' 섹션에서 상세히 다룹니다.  
// ...  
\`\`\`scala  
// src/main/scala/framework/spec/SpecRegistry.scala  
package framework.spec

import scala.collection.mutable.ListBuffer

/\*\*  
 \* SpecRegistry: 프레임워크의 중앙 데이터 레지스트리 오브젝트입니다.  
 \* 모든 스펙 정의 (\[\[HardwareSpecification\]\])와 \`@LocalSpec\` 태그 정보 (\[\[Tag\]\])가  
 \* 컴파일 시점에 이곳에 수집됩니다.  
 \*/  
object SpecRegistry {  
  private\[spec\] val specBuf: ListBuffer\[HardwareSpecification\] \= ListBuffer.empty\[HardwareSpecification\]  
  private\[spec\] val tagBuf: ListBuffer\[Tag\] \= ListBuffer.empty\[Tag\]

  /\*\*  
   \* 새로운 하드웨어 스펙 정의를 레지스트리에 추가합니다.  
   \* 동일 ID의 스펙이 중복 추가되는 경우 경고 메시지를 출력합니다.  
   \*/  
  def addSpec(spec: HardwareSpecification): Unit \= {  
    if (specBuf.exists(\_.id \== spec.id)) {  
      println(s"\[WARN SpecRegistry\] Duplicate spec ID added: ${spec.id}. Only the first definition will be used in final SpecIndex if aggregated.")  
    }  
    specBuf \+= spec  
  }

  /\*\*  
   \* \`@LocalSpec\` 어노테이션에 의해 생성된 태그 정보를 레지스트리에 추가합니다.  
   \* 동일한 Tag 객체 (id, srcFile, line, column 기준)가 중복 추가되는 경우 경고 메시지를 출력합니다.  
   \*/  
  def addTag(tag: Tag): Unit \= {  
    if (tagBuf.exists(t \=\> t.id \== tag.id && t.srcFile \== tag.srcFile && t.line \== tag.line && t.column \== tag.column)) {  
      println(s"\[WARN SpecRegistry\] Duplicate tag added for ID: ${tag.id} (Src: ${tag.srcFile}:${tag.line}). This may indicate redundant @LocalSpec annotations.")  
    }  
    tagBuf \+= tag  
  }

  /\*\*  
   \* 현재 레지스트리에 등록된 모든 하드웨어 스펙 정의를 반환합니다.  
   \*/  
  def allSpecs: Seq\[HardwareSpecification\] \= specBuf.toSeq

  /\*\*  
   \* 현재 레지스트리에 등록된 모든 태그 정보를 반환합니다.  
   \*/  
  def allTags: Seq\[Tag\] \= tagBuf.toSeq

  /\*\*  
   \* 레지스트리를 초기화합니다. (테스트 또는 빌드 클린업 용도)  
   \*/  
  def clear(): Unit \= {  
    specBuf.clear()  
    tagBuf.clear()  
  }  
}

## **2\. 핵심 구성 요소**

프레임워크는 다음 세 가지 핵심 구성 요소로 이루어집니다.

1. **스펙 정의 파일 (Scala DSL)**: .scala 파일 내에 정의되는 스펙입니다.  
   * **HardwareSpecification**: 모든 스펙 객체의 기본 타입입니다. 이제 모든 필드를 포함하는 \*\*단일 final case class\*\*입니다.  
   * **순수 함수형 Staged Builder DSL (object Spec)**: Spec.CONTRACT, Spec.FUNCTION 등 팩토리 메서드를 통해 스펙을 작성합니다.  
   * **val 변수명 및 id 필드**: 각 스펙은 val 변수로 선언되며, **id 필드를 통해 고유 ID를 명시적으로 부여**합니다. 스펙 정의 시 SpecRegistry에 자동으로 등록됩니다.  
2. **RTL 구현 태깅 (@LocalSpec 어노테이션)**: RTL 코드 내에서 특정 스펙이 구현되거나 검증되는 지점을 표시하는 Scala 어노테이션입니다.  
   * **@LocalSpec("\<스펙\_ID\_문자열\>")**: 어노테이션은 인자로 해당 스펙의 **고유 ID 문자열**을 직접 받습니다 (예: @LocalSpec("MY\_CONTRACT\_SPEC")).  
   * **매크로 어노테이션**: 컴파일 시 @LocalSpec 매크로가 스펙 ID와 RTL 코드의 파일/라인, Scala 선언 경로 등의 정보를 결합하여 SpecRegistry에 **Tag 정보**를 등록합니다.  
3. **빌드 및 분석 파이프라인**: 스펙 파일과 RTL 코드로부터 스펙 정보를 추출하고, 검증하며, 보고서를 생성하는 자동화된 도구 모음입니다.  
   * **Scala 컴파일**: @LocalSpec 매크로 어노테이션 확장이 이 단계에서 이루어지며, 스펙 태그 정보가 SpecRegistry에 등록됩니다.  
   * **FIRRTL Elaboration**: RTL 인스턴스 경로와 같은 정확한 하드웨어 계층 정보가 SpecRegistry에 등록된 Tag 정보에 추가됩니다.  
   * **exportSpecIndex (SBT Task)**: SpecRegistry에 수집된 모든 HardwareSpecification 객체와 Tag 정보를 결합하여 SpecIndex.json과 TagIndex.json 파일을 생성합니다.
   * **specLint (SBT Task, Optional)**: SpecIndex.json을 기반으로 스펙 누락, 사용되지 않는 스펙 등 스펙 관련 규칙을 검사하고 경고/오류를 발생시킵니다.  
   * **Verification Run**: 검증 도구들이 검증 결과 로그를 생성합니다.  
   * **reportGen**: JSON 파일과 검증 로그를 병합하여 HTML 대시보드 및 커버리지 보고서를 생성합니다.

### **2.1. 매크로 패턴 커버리지**

@LocalSpec 어노테이션은 다음 Scala 선언에 직접 적용할 수 있습니다.

| 대상 | 설명 | 예시 |
| :---- | :---- | :---- |
| class | Chisel Module 클래스 정의 전체에 대한 스펙 (주로 CONTRACT 스펙) | @LocalSpec("MY\_MODULE\_CONTRACT") class MyModule extends Module { ... } |
| object | 싱글톤 오브젝트에 대한 스펙 (예: 전역 설정, 인터페이스 정의) | @LocalSpec("GLOBAL\_CONFIG\_SPEC") object GlobalConfig { ... } |
| val | 특정 값, 로직 블록, Assertion 또는 FSM 상태에 대한 스펙 | @LocalSpec("PIPELINE\_PROP") val pipeAssert \= assert(...) |
| def | 특정 함수 또는 메서드에 대한 스펙 (Chisel def 정의) | @LocalSpec("CALC\_FUNCTION") def calculateNextState(s: UInt): UInt \= { ... } |

### **2.2. SpecPathTransform 및 FIRRTL Annotation 보강 (향후 구현)**

매크로를 통해 삽입된 @LocalSpec 정보는 초기에는 소스 파일 경로, 라인 번호, 그리고 **Scala 선언 경로**만 가집니다. **FIRRTL Transform 단계에서 fullyQualifiedModuleName 및 hardwareInstancePath와 같은 RTL 계층 정보를 보강합니다.**

**작동 방식:**

1. **매크로**: @LocalSpec 매크로는 어노테이션 인자로 받은 스펙 ID와 현재 소스 파일/라인, \*\*Scala 선언 경로(scalaDeclarationPath)\*\*를 포함하는 Tag 객체를 SpecRegistry에 등록합니다. 이 단계에서는 fullyQualifiedModuleName과 hardwareInstancePath는 **플레이스홀더**("PLACEHOLDER\_MODULE", "PLACEHOLDER\_PATH")로 남겨둡니다.  
2. **FIRRTL Elaboration**: Chisel 코드로부터 FIRRTL 중간 표현(IR)이 생성됩니다. 이때 FIRRTL 컴파일러 플러그인(또는 커스텀 Transform)이 IR 트리를 순회하며 각 모듈 인스턴스의 실제 계층 경로를 추적합니다.  
3. **SpecPathTransform**: 이 Transform은 SpecRegistry에 등록된 Tag 정보 중 플레이스홀더로 남아있는 fullyQualifiedModuleName과 hardwareInstancePath 필드를 실제 FIRRTL IR에서 파악된 정보로 업데이트합니다. 예를 들어, Queue 클래스의 인스턴스가 top.subsys.myQueue 경로에 있다면, 해당 Tag의 hardwareInstancePath를 이 경로로 채웁니다.

이 과정을 통해 SpecIndex.json 및 TagIndex.json에 포함될 instancePaths 정보가 정확하게 채워집니다.

### **2.3. IDE 통합의 제약 사항 및 해결 방안**

현재 @LocalSpec 어노테이션이 스펙 ID를 문자열 리터럴로 받기 때문에, IDE는 이 문자열과 해당 스펙 정의(val MY\_SPEC \= ...) 사이에 직접적인 의미론적 연결을 자동으로 제공하지 않습니다. 즉, IDE에서 @LocalSpec("MY\_SPEC\_ID")의 "MY\_SPEC\_ID"를 클릭하여 val MY\_SPEC 정의로 바로 이동하는 기능은 지원되지 않습니다.

**대안 및 향후 계획:**

* **SpecIndex.json**: exportSpecIndex 태스크가 생성하는 SpecIndex.json 파일은 **스펙 ID와 해당 스펙 정의, 그리고 스펙이 태그된 모든 코드 위치 정보를 연결하는 단일 정보원**입니다. 개발자는 이 JSON 파일을 통해 스펙 정보를 조회할 수 있습니다.  
* **보고서 생성 도구 (reportGen)**: 나중에 reportGen 태스크가 HTML/PDF 보고서를 생성하면, 그 보고서 안에서 스펙 ID를 클릭하면 해당 스펙이 정의된 위치나 태그된 코드 위치로 이동하는 **하이퍼링크 기능**을 구현할 수 있습니다.  
* **scalaDeclarationPath 활용**: Tag에 포함된 scalaDeclarationPath는 IDE 확장 기능이나 커스텀 스크립트에서 스펙 ID와 함께 사용되어, 사용자가 스펙 정의 위치를 보다 정확하게 추적할 수 있도록 돕습니다.  
* **Scala 3 마이그레이션**: Scala 3의 scala.quoted 매크로 시스템은 이러한 타입 안전성 및 IDE 통합 제약을 극복할 수 있는 새로운 메타프로그래밍 API를 제공합니다. 장기적으로 Scala 3으로 마이그레이션한다면, @LocalSpec(MyExampleSpecs.QueueSpec)와 같이 HardwareSpecification 객체를 직접 매크로 인자로 전달하여 IDE 지원을 개선할 수 있을 것입니다.

## **3\. 스펙 문서 작성 가이드**

스펙 문서는 Scala 파일(.scala) 내에서 **순수 함수형 Staged Builder 기반의 DSL**을 사용합니다. 스펙이 정의될 때 자동으로 SpecRegistry에 등록되므로, 모든 스펙을 Seq로 묶어 반환할 필요가 없습니다.

### **3.1. 기본 구조**

모든 스펙은 Spec.\<CATEGORY\_KEYWORD\> 팩토리 함수로 시작하며, val 변수에 할당됩니다. 스펙 정의는 .build() 메서드 호출로 마무리됩니다.

// src/main/scala/your\_project/specs/MyExampleSpecs.scala

package your\_project.specs

import framework.spec.\_  
import Spec.\_ // Spec 오브젝트의 팩토리 메서드를 직접 사용하기 위해 임포트

/\*\*  
 \* 새로운 Staged Builder DSL을 사용한 HardwareSpecification 정의 예시입니다.  
 \*/  
object MyExampleSpecs {  
  val QueueCap \= Capability("Queueing") // Capability 정의

  // FUNCTION 스펙 정의 예시  
  val QueueSpec: HardwareSpecification \= Spec.FUNCTION(  
    id \= "QUEUE\_FUNC\_001",  
    desc \= "Queue must preserve FIFO order for all enqueued elements."  
  ).capability(QueueCap)             // .hasCapability 대신 .capability  
   .status("DRAFT")                  // .withStatus 대신 .status  
   .meta("author" \-\> "String.Alice", "priority" \-\> "high") // .withMetadata 대신 .meta, varargs 사용  
   .entry("Purpose", "Ensures data integrity during queue operations.")  
   .entry("Algorithm", "FIFO")  
   .build()                           // 최종적으로 .build() 호출 (괄호 없음)

  // PROPERTY 스펙 정의 예시  
  val ResetSpec: HardwareSpecification \= Spec.PROPERTY(  
    id \= "RESET\_PROP\_001",  
    desc \= "Module must reset all internal state to zero on reset signal."  
  ).noCapability                     // capability가 없을 때 .noCapability 호출  
   .status("APPROVED")  
   .meta("author" \-\> "String.Bob")  
   .entry("ResetType", "Synchronous")  
   .entry("ResetValue", "Zero")  
   .build()  
}

### **3.2. 핵심 스펙 필드 및 DSL 메서드**

각 스펙 정의는 Spec.\<CATEGORY\_KEYWORD\>(id, desc)로 시작하며, Stage1 및 Stage2 빌더 객체의 메서드들을 체이닝으로 사용하여 필드를 설정합니다. 마지막에 .build()를 호출하여 최종 HardwareSpecification 객체를 생성합니다.

* **id \= "\<스펙\_ID\_문자열\>"**: **필수**. 스펙의 고유 식별자를 문자열로 명시합니다.  
* **desc \= """...""".stripMargin.trim**: **필수**. 스펙의 상세 내용을 여러 줄 텍스트로 작성합니다. Markdown 문법을 사용할 수 있습니다.  
* **.capability(Capability("역할\_이름"))**: (선택 사항) 해당 스펙이 나타내는 추상적인 기능 역할을 명시합니다. framework.spec.Capability 객체를 사용합니다. Spec DSL의 Stage1에서 capability 또는 noCapability를 호출하여 다음 단계로 진입해야 합니다.  
* **.noCapability**: (선택 사항) 스펙에 특정 기능 역할이 없음을 명시하고 다음 단계로 진입합니다.  
* **.parents(ids: String\*)**: 해당 스펙이 종속되거나 하위 속하는 \*\*부모 스펙의 ID(문자열)\*\*를 명시합니다. varargs를 사용하여 여러 ID를 한 번에 추가할 수 있습니다.  
* **.related(ids: String\*)**: 직접적인 부모는 아니지만, 개념적으로 밀접하게 관련된 다른 스펙 ID들을 나열합니다. varargs를 사용하여 여러 ID를 한 번에 추가할 수 있습니다.  
* **.status(s: String)**: 스펙의 현재 상태를 명시합니다 ("DRAFT", "APPROVED", "STABLE" 등).  
* **.impl(by: String)**: 스펙 구현자를 설정합니다 (예: design.frontend.FetchUnit).  
* **.verified(by: String)**: 스펙 검증자를 설정합니다 (예: verification.testbench.FetchUnitTB).
* **.is(refs: HardwareSpecification\*)**: 지정한 다른 스펙 객체와 동등하거나 상속 관계임을 명시합니다. 문자열 ID 대신 스펙 객체를 직접 전달할 수 있습니다.
* **.has(refs: HardwareSpecification\*)**: 현재 스펙이 포함하는 하위 스펙들을 객체 참조 형태로 선언합니다.
* **.uses(refs: HardwareSpecification\*)**: CONTRACT 스펙이 의존하거나 사용하는 다른 CONTRACT 스펙을 지정합니다. 스펙 객체 또는 ID를 모두 지원합니다.
* **.requiresCaps(ids: String\*)**: 이 스펙(보통 CONTRACT 스펙)이 자신의 기능을 수행하기 위해 요구하는 Capability ID 목록을 명시합니다. varargs를 사용하여 여러 ID를 한 번에 추가할 수 있습니다.
* **.meta(kv: (String,String)\*)**: 스펙에 대한 추가적인 구조화된 정보를 키-값 쌍으로 추가합니다. varargs를 사용하여 여러 쌍을 한 번에 추가할 수 있습니다.  
* **.entry(name: String, value: String)**: 스펙의 상세 항목을 이름-값 쌍으로 추가합니다. HardwareSpecification의 entries 필드를 채웁니다. 여러 번 호출하여 여러 엔트리를 추가할 수 있습니다.  
* **.build()**: 스펙 정의 체이닝의 **마지막 메서드**입니다. 이 메서드가 호출될 때 최종 HardwareSpecification 객체가 생성되고, MetaFile에 기록되며 SpecRegistry에 등록됩니다.

### **3.3. 스펙 카테고리**

Spec 오브젝트에서 제공하는 팩토리 함수를 사용하여 스펙 카테고리를 명시합니다.

* **Spec.CONTRACT(id, desc)**: 모듈 또는 서브 모듈의 최상위 계약을 정의합니다.  
* **Spec.FUNCTION(id, desc)**: 특정 기능 블록이나 동작에 대한 스펙입니다.  
* **Spec.PROPERTY(id, desc)**: 시스템이 항상 만족해야 하는 불변 속성이나 제약 조건입니다.  
* **Spec.COVERAGE(id, desc)**: 특정 시나리오나 상태가 검증 과정에서 도달해야 함을 정의합니다.  
* **Spec.INTERFACE(id, desc)**: 모듈 간의 인터페이스나 프로토콜 정의에 사용합니다.  
* **Spec.PARAMETER(id, desc)**: 디자인의 중요한 설정 파라미터에 대한 스펙입니다.  
* **Spec.RAW(id, desc, prefix)**: 특정 다이어그램, 파형, 용어 정의 등 자유로운 형식의 문서적 스펙입니다. prefix는 다이어그램의 종류("Mermaid") 등을 명시할 수 있습니다.

## **4\. RTL 구현 가이드: @LocalSpec 태깅**

RTL 코드(Chisel Scala)에서 스펙이 구현되거나 검증되는 지점을 명시하기 위해 @LocalSpec 어노테이션을 사용합니다.

### **4.1. @LocalSpec 사용법**

@LocalSpec 어노테이션은 스펙의 **고유 ID 문자열**을 인자로 받습니다.

// src/main/scala/your\_project/design/Queue.scala

package your\_project.design

import chisel3.\_  
import framework.macros.LocalSpec // @LocalSpec 매크로 임포트

// \--- CONTRACT 스펙 태그: 클래스(모듈)에 직접 적용 \---  
// 이 클래스(Module) 자체가 KLASE32\_FRONTEND\_CONTRACT 스펙을 구현함을 명시합니다.  
@LocalSpec("KLASE32\_FRONTEND\_CONTRACT") // 스펙 ID 문자열로 직접 전달  
class Queue(depth: Int \= 4, w: Int \= 32\) extends Module {  
  val io \= IO(new Bundle {  
    val enq \= Flipped(Decoupled(UInt(w.W)))  
    val deq \= Decoupled(UInt(w.W))  
  })

  // 큐 내부 로직 (예시)  
  val q \= Reg(Vec(depth, UInt(w.W)))  
  val enqPtr \= RegInit(0.U(log2Ceil(depth \+ 1).W))  
  val deqPtr \= RegInit(0.U(log2Ceil(depth \+ 1).W))  
  val maybeFull \= RegInit(false.B)

  val ptr\_match \= enqPtr \=== deqPtr  
  val empty \= ptr\_match && \!maybeFull  
  val full \= ptr\_match && maybeFull

  io.enq.ready := \!full  
  io.deq.valid := \!empty

  when (io.enq.fire) {  
    q(enqPtr) := io.enq.bits  
    enqPtr := enqPtr \+ 1.U  
  }  
  when (io.deq.fire) {  
    deqPtr := deqPtr \+ 1.U  
  }  
  when (io.enq.fire \=/= io.deq.fire) {  
    maybeFull := io.enq.fire  
  }

  io.deq.bits := q(deqPtr)

  // \--- PROPERTY 스펙 구현 태그 \---  
  // assert, assume 등 특정 불변 속성을 정의하는 val/def에 태그합니다.  
  @LocalSpec("FETCH\_UNIT\_PIPELINE\_PROPERTY") // 스펙 ID 문자열로 직접 전달  
  val queuePropertyAssertion \= assert(\!(full && io.enq.ready), "Queue must not be full when enq.ready is asserted")

  // \--- FUNCTION 스펙 구현 태그 \---  
  // 특정 기능 블록이나 중요한 로직을 캡슐화하는 val/def에 태그합니다.  
  @LocalSpec("AXI4\_LITE\_INTERFACE") // 스펙 ID 문자열로 직접 전달  
  val interfaceLogic \= {  
    // ... AXI4-Lite 인터페이스 관련 로직이 여기에 올 수 있습니다 ...  
    val someInterfaceValue \= io.enq.bits \+ 1.U  
    someInterfaceValue  
  }

  // \--- PARAMETER 스펙 구현 태그 \---  
  // 모듈의 파라미터가 어떤 스펙을 따르는지 명시하는 val/def에 태그합니다.  
  @LocalSpec("AXI4\_LITE\_INTERFACE") // 예시: 이 depth 파라미터가 AXI4-LITE 인터페이스에 영향을 줌  
  val myQueueDepth \= depth  
}

### **4.2. 태깅 원칙**

* **기본 태깅 대상**: @LocalSpec는 Scala의 **class (모듈), object, val, def 선언에 태그하는 것이 기본**입니다.  
* **스펙 ID 참조**: 각 @LocalSpec 태그는 **스펙 정의 파일(MyExampleSpecs.scala 등)에 존재하는 유효한 HardwareSpecification의 id 문자열**을 참조해야 합니다. SpecRegistry는 이 ID를 통해 Tag 정보와 실제 HardwareSpecification 정의를 연결합니다.  
* **적절한 위치**: 스펙이 구현되거나 검증되는 가장 의미 있는 코드 라인 또는 블록에 @LocalSpec를 태그합니다.  
* **더미 val 사용**: class 선언에 직접 태그하는 것을 제외하고, 코드 블록 내부의 특정 로직에 태그를 달기 위해서는 val dummyTag \= { ... } 또는 val dummyTag \= ()와 같은 **더미 val 선언을 플레이스홀더**로 사용해야 합니다.  
* **컨트랙트 스펙**: 각 RTL 모듈(클래스)에는 해당 모듈의 최상위 계약을 정의하는 **최소 하나의 CONTRACT 스펙**이 @LocalSpec로 태깅되어야 합니다. 이 규칙은 specLint에 의해 강제될 수 있습니다.

### **4.3. when().localtag() 와 같은 DSL의 한계**

현재 Scala 2.13.12의 scala-reflect 매크로 한계와 Chisel의 내부 구조를 고려할 때, when().localtag()와 같은 복잡한 DSL을 **직접적으로 지원하기는 어렵습니다.** @LocalSpec 어노테이션은 val 또는 class와 같은 **선언**에만 적용될 수 있습니다. when 블록 내부의 스펙은 해당 블록을 감싸는 val에 태그하는 방식을 유지하는 것이 현실적입니다.

## **5\. 빌드 시스템 연동 (SBT 가이드)**

프레임워크는 spec-core, spec-macros 및 spec-plugin SBT 서브 프로젝트를 통해 빌드 시스템과 통합됩니다. 이 문서에서는 사용자 관점에서 필요한 최소한의 정보를 제공합니다. 자세한 빌드 설정 및 태스크 실행 방법은 "하드웨어 스펙 관리 프레임워크: 개발자 전문 가이드" 문서를 참조해주세요.

## **6\. 스펙 현황 보고서 (exportSpecIndex, specLint, reportGen)**

exportSpecIndex SBT 태스크는 SpecRegistry에 수집된 HardwareSpecification 객체와 Tag 객체를 기반으로 두 개의 JSON 파일을 생성합니다.

* **SpecIndex.json**: 모든 스펙 정의(HardwareSpecification에서 온 정보)와 해당 스펙이 태그된 RTL 코드의 위치 정보(Tag에서 온 정보)를 연결하여 상세 보고서의 단일 정보원을 제공합니다. 여기서 스펙 id는 스펙 정의와 태그를 연결하는 핵심 키 역할을 합니다. 각 태그된 위치에는 scalaDeclarationPath (어노테이션이 위치한 Scala 선언의 완전 경로)도 포함됩니다.  
* **TagIndex.json**: 각 RTL 모듈(fullyQualifiedModuleName)별로 어떤 스펙들이 태그되어 있는지 요약 정보를 제공하며, Tag 객체들의 리스트가 아닌 모듈 이름을 키로 하는 **JSON 객체(Map\[String, Map\[String, List\[String\]\]) 형태**로 생성됩니다.

이 JSON 파일들은 specLint 태스크를 통해 스펙의 정합성을 검사하고, reportGen 태스크를 통해 HTML/PDF 보고서 등 시각적인 스펙 현황 보고서를 생성하는 데 활용됩니다.