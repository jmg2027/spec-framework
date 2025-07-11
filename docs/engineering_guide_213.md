# **하드웨어 스펙 관리 프레임워크: 개발자 전문 가이드 (Scala 2.13.12 버전)**

릴리즈 버전: 1.0.8 (종합 업데이트 \- 네이밍, 매크로 동작, 파일 구조, 트러블슈팅)  
릴리즈 날짜: 2025-06-29

## **1\. 개요 및 목적**

이 문서는 하드웨어 스펙 관리 프레임워크를 **직접 개발하고 유지보수하며 확장할 개발자**를 위한 전문 가이드입니다. 프레임워크의 내부 아키텍처, 핵심 구현 상세, 기술적 결정 사항, 그리고 각 구성 요소의 연동 방식에 대한 심층적인 이해를 돕는 것을 목표로 합니다.

**프레임워크의 목표 (재확인):**

* 하드웨어 스펙을 타입 안전하고 컴파일러가 검증하는 코드로 정의  
* RTL 구현과 스펙을 긴밀하게 연결 (태깅)  
* 자동화된 도구 체인을 통해 스펙-코드 정합성 및 커버리지 관리  
* Scala 2.13.12의 scala-reflect 기반 매크로 활용

## **2\. 프레임워크 아키텍처 (개발자 관점)**

프레임워크는 크게 세 가지 SBT 서브 프로젝트로 구성되며, 이들은 서로 의존하며 스펙 관리 파이프라인을 형성합니다.

* **spec-core**: 프레임워크의 핵심 데이터 모델 (스펙 정의, 태그 정보) 및 중앙 레지스트리 (SpecRegistry)를 포함합니다.  
* **spec-macros**: RTL 코드에 스펙을 태깅하는 @LocalSpec 어노테이션 매크로를 구현합니다.  
* **spec-plugin**: 스펙 정보 추출 및 JSON 보고서 생성을 위한 SBT 태스크를 정의합니다.

**빌드 파이프라인 개요:**

graph TD  
    A\[스펙 정의 파일\] \--\>|1. DSL 정의 (object Spec)| B(spec-core)  
    C\[RTL 코드\] \--\>|2. @LocalSpec 태깅| D(design 모듈)  
    D \--\>|3. Scala 컴파일 & 매크로 확장| E(컴파일된 코드)  
    E \--\>|4. @LocalSpec 매크로 실행| F(SpecRegistry에 Tag 등록)  
    B \--\>|초기 스펙 등록 (.build() 호출 시)| F  
    F \--\>|5. FIRRTL Elaboration (향후 구현)| G(FIRRTL IR with enriched Tags)  
    G \--\>|6. SpecPathTransform (향후 구현)| H(최종 Tag 정보 보강)  
    H & F \--\>|7. exportSpecIndex SBT Task| I(SpecIndex.json & TagIndex.json 생성)
    I \--\>|8. specLint SBT Task (향후 구현)| J(스펙 정합성 검사)  
    I & K\[외부 검증 로그\] \--\>|9. reportGen SBT Task (향후 구현)| L(HTML/PDF 보고서)

## **3\. spec-core 모듈 상세**

spec-core는 프레임워크의 데이터 계약을 담당합니다. 이 모듈의 핵심 타입들은 모두 framework.spec 패키지 아래에 정의됩니다.

### **3.1. 핵심 데이터 모델 (framework.spec 패키지 내)**

* **HardwareSpecification**: 모든 스펙의 기본 타입입니다. 이제 모든 필드를 포함하는 \*\*단일 final case class\*\*입니다. (일반적으로 HardwareSpecification.scala 파일에 정의)  
  // spec-core/src/main/scala/framework/spec/HardwareSpecification.scala  
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
    implicit val rw: upickle.default.ReadWriter\[HardwareSpecification\] \= upickle.default.macroRW  
  }

  }

* **SpecCategory**: 스펙 유형을 분류하는 sealed trait와 object. (일반적으로 HardwareSpecification.scala 파일에 HardwareSpecification과 함께 정의)  
  // spec-core/src/main/scala/framework/spec/HardwareSpecification.scala (SpecCategory 부분 발췌)  
  sealed trait SpecCategory  
  object SpecCategory {  
    case object CONTRACT extends SpecCategory  
    case object FUNCTION extends SpecCategory  
    case object PROPERTY extends SpecCategory  
    case object COVERAGE extends SpecCategory  
    case object INTERFACE extends SpecCategory  
    case object PARAMETER extends SpecCategory  
    case class RAW(prefix: String) extends SpecCategory

    // SpecCategory의 ReadWriter는 ujson.Str을 직접 사용하여 문자열 매핑 문제를 해결합니다.  
    implicit val rw: upickle.default.ReadWriter\[SpecCategory\] \= upickle.default.ReadWriter\[SpecCategory\](  
      upickle.default.Writer\[SpecCategory\] {  
        case CONTRACT \=\> ujson.Str("CONTRACT")  
        case FUNCTION \=\> ujson.Str("FUNCTION")  
        case PROPERTY \=\> ujson.Str("PROPERTY")  
        case COVERAGE \=\> ujson.Str("COVERAGE")  
        case INTERFACE \=\> ujson.Str("INTERFACE")  
        case PARAMETER \=\> ujson.Str("PARAMETER")  
        case RAW(prefix) \=\> ujson.Str(s"RAW:$prefix")  
      },  
      upickle.default.Reader\[SpecCategory\] {  
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

* **Capability**: 추상적 기능 역할을 나타내는 case class. (일반적으로 HardwareSpecification.scala 파일에 함께 정의)  
  // spec-core/src/main/scala/framework/spec/HardwareSpecification.scala (Capability 부분 발췌)  
  final case class Capability(name: String)  
  object Capability {  
    implicit val rw: upickle.default.ReadWriter\[Capability\] \= upickle.default.macroRW  
  }

* **Tag**: @LocalSpec 매크로에 의해 생성되는 메타데이터를 담는 case class. (일반적으로 Tag.scala 파일에 정의)  
  // spec-core/src/main/scala/framework/spec/Tag.scala  
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
    implicit val rw: upickle.default.ReadWriter\[Tag\] \= upickle.default.macroRW  
  }

* **Spec (object Spec)**: 스펙 정의를 위한 **순수 함수형 Staged Builder DSL**입니다. (일반적으로 Spec.scala 파일에 정의)  
  * **기존 SpecBuilder 및 Specs 객체를 완전히 대체**합니다.  
  * 내부적으로 불변 case class Core를 사용하여 스펙 데이터를 집계하고, 각 빌더 메서드는 Core의 복사본을 포함하는 새로운 빌더 인스턴스를 반환합니다.  
  * 최종 build() 메서드 호출 시 HardwareSpecification 객체가 생성되고 MetaFile에 파일이 기록됩니다.  
* **SpecRegistry**: 프레임워크의 중앙 데이터 레지스트리. (일반적으로 SpecRegistry.scala 파일에 정의)  
  * specBuf: mutable.ListBuffer\[HardwareSpecification\]와 tagBuf: mutable.ListBuffer\[Tag\]를 사용하여 스펙과 태그를 수집합니다.  
  * **java.util.concurrent 컬렉션 대신 일반 mutable.ListBuffer를 사용**: 빌더가 단일 스레드 컴파일러 컨텍스트에서 실행되므로 불필요한 동시성 오버헤드를 제거했습니다.  
  * addSpec, addTag 메서드는 중복 추가 시 경고 메시지를 출력합니다.

### **3.2. MetaFile (framework.spec.MetaFile.scala에 정의)**

MetaFile은 컴파일 타임에 스펙 및 태그 메타데이터를 JSON 파일로 내보내는 유틸리티 object입니다.

* **경로 설정 개선**: spec.meta.dir 시스템 속성에 의존하는 대신, MetaFile.setBaseDir(path: Path) 메서드를 통해 **SBT build.sbt의 resourceGenerators 태스크에서 명시적으로 출력 디렉토리를 주입**하도록 변경되었습니다. 이는 경로 설정의 견고성을 크게 높였습니다.  
  * **주의**: scalacOptions \+= s"-Dspec.meta.dir=${...}" 설정은 이제 MetaFile에서 직접적으로 사용되지 않지만, 다른 매크로 또는 컴파일러 플러그인에 필요할 수 있으므로 유지될 수 있습니다. MetaFile은 setBaseDir를 통한 명시적 주입을 우선시합니다.

### **3.3. SpecIndexEntry (framework.spec.SpecIndexEntry.scala에 정의)**

SpecIndex.json 파일의 각 항목을 나타내는 case class입니다.

* scalaDeclarationPath: Option\[String\] 필드가 추가되어 Tag의 scalaDeclarationPath 정보를 JSON에 포함합니다.

## **4\. spec-macros 모듈 상세**

spec-macros는 핵심적인 @LocalSpec 어노테이션 매크로를 포함합니다.

### **4.1. @LocalSpec 어노테이션 매크로**

* **선언:** class LocalSpec(specId: String) extends scala.annotation.StaticAnnotation  
  * **중요 변경 사항:** @LocalSpec는 이제 HardwareSpecification 객체 대신 **스펙의 고유 ID 문자열(specId: String)을 직접 인자로 받습니다.**  
  * **변경 이유:** Scala 2의 scala-reflect 매크로 (c.eval())는 컴파일 중인 프로젝트 내 정의된 복합 객체(예: val QueueSpec)를 컴파일 시점에 안전하게 평가하는 데 제약이 있습니다. 문자열 리터럴 사용은 이 제약을 회피하고 안정성을 높입니다.  
  * macroTransform 메서드는 LocalSpec.impl 매크로를 가리킵니다.  
* **LocalSpec.impl 매크로 구현:**  
  * 어노테이션의 인자로부터 specId 문자열을 추출합니다.  
  * c.enclosingPosition.source.path, line, column을 사용하여 소스 코드 위치를 추출합니다.  
  * **c.enclosingOwner.fullName을 사용하여 @LocalSpec 어노테이션이 적용된 Scala 선언(클래스, 오브젝트, val, def)의 완전한 Scala 경로를 추출하고, 이를 Tag 객체의 scalaDeclarationPath 필드에 주입합니다.**  
  * 생성된 Tag 객체를 SpecRegistry.addTag() 메서드를 통해 중앙 레지스트리에 등록하는 Scala 코드를 어노테이션이 붙은 지점에 삽입합니다. 이 코드는 컴파일된 .class 파일에 포함되어 **컴파일 타임에 실행**됩니다.  
  * val, def, class, object 선언에 @LocalSpec를 적용할 수 있도록 패턴 매칭 로직을 구현합니다. 각 경우에 대해 원래의 코드 구조를 유지하면서 Tag 등록 코드를 적절한 위치에 삽입합니다.

## **5\. spec-plugin 모듈 상세**

spec-plugin은 SBT 빌드 시스템에 통합되어 스펙 정보 추출 및 보고서 생성을 자동화합니다.

### **5.1. SBT 태스크 정의**

* **exportSpecIndex (taskKey)**:  
  * **역할:** SpecRegistry에 수집된 HardwareSpecification 객체들과 Tag 객체들을 조합하여 SpecIndex.json과 TagIndex.json 파일을 생성합니다.
  * **구현 상세:**  
    * SpecRegistry.allSpecs에서 모든 스펙 정의를 가져옵니다.  
    * SpecRegistry.allTags에서 @LocalSpec 태그 정보를 가져옵니다.  
    * 두 정보를 스펙 id를 기준으로 매핑하여 각 스펙 엔트리(SpecIndex.json의 각 원소)에 해당 instancePaths 정보를 채워 넣습니다.  
    * SpecIndexEntry 케이스 클래스의 macroRW를 사용하여 스펙 데이터와 Tag 데이터를 SpecIndex.json으로 직렬화합니다. instancePaths 내 각 맵에 scalaDeclarationPath 필드를 포함합니다.  
    * TagIndex.json은 allTags를 기준으로 모듈 이름(fullyQualifiedModuleName)을 키로 하는 **맵 형태**(Map\[String, Map\[String, List\[String\]\]\])로 스펙 목록을 집계하여 생성합니다.
    * MetaFile.setBaseDir 호출을 통해 design/build.sbt에서 exportSpecIndex 태스크 실행 시 출력 디렉토리가 명시적으로 MetaFile에 주입되도록 합니다.  
  * upickle.default.write를 사용하여 JSON 형식으로 직렬화하고 파일로 저장합니다.  
* **specLint (taskKey, 향후 구현)**: SpecIndex.json을 기반으로 스펙 정합성을 검사합니다.  
* **reportGen (taskKey, 향후 구현)**: JSON 파일과 외부 검증 로그를 병합하여 HTML 대시보드 및 커버리지 보고서를 생성합니다.

### **5.2. SBT 프로젝트 구조 및 소스 디렉토리 규칙**

프레임워크는 멀티-프로젝트 SBT 구조를 사용하며, 특히 Scala 버전별 소스 코드 관리를 위해 다음과 같은 규칙을 따릅니다.

**표준 프로젝트 구조:**

project-root/  
├── build.sbt                     \<-- (1) 루트 빌드 정의 파일 (멀티-프로젝트 설정)  
├── project/                      \<-- (2) SBT 빌드 자체를 위한 파일들  
│   └── plugins.sbt               \<-- (2a) SBT 플러그인 의존성 정의  
│  
├── spec-core/                    \<-- (3) 스펙 프레임워크의 핵심 데이터 모델 및 레지스트리  
│   ├── src/  
│   │   └── main/  
│   │       └── scala/            \<-- (3a) spec-core의 Scala 소스 코드 (모든 Scala 버전에서 공통)  
│   │           └── framework/  
│   │               └── spec/  
│   │                   ├── HardwareSpecification.scala  
│   │                   ├── Generated.scala  
│   │                   ├── MetaFile.scala  
│   │                   ├── Spec.scala  
│   │                   ├── SpecIndexEntry.scala  
│   │                   ├── SpecRegistry.scala  
│   │                   └── Tag.scala  
│   ├── scala-2.12/               \<-- (\!\!\!제거됨\!\!\!) Scala 2.12 특정 소스 코드 (이전 SpecBuilder/SpecRegistry)  
│   ├── scala-2.13/               \<-- (\!\!\!제거됨\!\!\!) Scala 2.13 특정 소스 코드 (이전 SpecBuilder/SpecRegistry)  
│   └── build.sbt                 \<-- (3b) spec-core 서브 프로젝트 빌드 정의  
│  
├── spec-macros/                  \<-- (4) @LocalSpec 어노테이션 매크로 구현 (버전별 구현 분리)  
│   ├── src/  
│   │   └── main/  
│   │       ├── scala-2.13/       \<-- (4a) Scala 2.13용 매크로 구현 (현재 사용 중인 LocalSpec.scala)  
│   │       │   └── framework/  
│   │       │       └── macros/  
│   │       │           └── LocalSpec.scala  
│   │       └── scala-3/          \<-- (4b) Scala 3용 매크로 구현 (미래 확장용, 현재 비어 있거나 스텁)  
│   │           └── framework/  
│   │               └── macros/  
│   │                   └── LocalSpec.scala  
│   └── build.sbt                 \<-- (4c) spec-macros 서브 프로젝트 빌드 정의  
│  
├── spec-plugin/                  \<-- (5) SBT 빌드 태스크 정의 (스펙 추출, 린트, 보고서)  
│   ├── src/  
│   │   └── main/  
│   │       └── scala/            \<-- (5a) spec-plugin의 Scala 소스 코드 (SBT 플러그인 로직)  
│   │           └── spec/  
│   │               └── plugin/  
│   │                   └── SpecPlugin.scala  
│   └── build.sbt                 \<-- (5b) spec-plugin 서브 프로젝트 빌드 정의  
│  
└── design/                       \<-- (6) 실제 RTL 디자인 코드 및 사용자 정의 스펙 (사용자 프로젝트)  
    ├── src/  
    │   └── main/  
    │       └── scala/            \<-- (6a) 사용자 RTL 및 스펙 정의 Scala 코드  
    │           └── your\_project/  
    │               ├── design/  
    │               │   └── MyCpu.scala        (예: Chisel Module 정의)  
    │               │   └── Queue.scala  
    │               └── specs/  
    │                   └── MyExampleSpecs.scala   (예: DSL로 정의된 스펙 파일)  
    ├── project/                  \<-- (NEW): design 프로젝트만을 위한 로컬 plugins.sbt 디렉토리  
    │   └── plugins.sbt           \<-- (NEW): spec-plugin 로드를 위한 로컬 plugins.sbt  
    └── build.sbt                 \<-- (6b) design 서브 프로젝트 빌드 정의

**소스 디렉토리 컴파일 규칙:**

* **src/main/scala-\<Scala Binary Version\>/**: SBT는 프로젝트의 scalaVersion (또는 crossScalaVersions에 정의된 각 버전)에 해당하는 이 디렉토리의 소스 파일들을 우선적으로 컴파일합니다. (예: scalaVersion := "2.13.12"면 src/main/scala-2.13/의 코드).  
* **src/main/scala/**: 이 디렉토리는 **모든 Scala 버전에서 공통적으로 사용될 수 있는 소스 코드**를 담습니다. SBT는 특정 scalaVersion으로 빌드할 때, 해당 scala-\<버전\>/ 디렉토리의 코드와 **함께** scala/ 디렉토리의 코드도 컴파일 대상에 포함합니다.  
  * **중요:** src/main/scala/와 src/main/scala-\<버전\>/ **두 곳 모두에 동일한 패키지 경로 및 파일명으로 정의된 클래스나 객체가 있다면, 컴파일 에러(중복 정의)가 발생합니다.** 따라서 scala/에는 모든 버전에 공통적인 코드만, 버전별로 구현이 달라지는 코드(예: 매크로 구현)는 반드시 scala-\<버전\>/ 디렉토리에만 두어야 합니다.

**build.sbt 파일들 (최종 및 완벽 수정 버전):**

**1\. project-root/build.sbt**

// build.sbt  
// 이 파일은 전체 멀티-프로젝트 빌드의 전역 설정과 서브 프로젝트 정의를 담당합니다.  
// 개별 프로젝트의 세부 설정(crossScalaVersions, scalacOptions 등)은 각 서브 프로젝트의 build.sbt 파일에서 합니다.

// \---------- Global Settings \----------  
ThisBuild / scalaVersion := "2.13.12" // 디자인 프로젝트 및 기본 Scala 버전  
ThisBuild / organization := "your.company"  
ThisBuild / version      := "0.1.0-SNAPSHOT"

// \---------- Sub-projects Definitions \----------

// spec-core: 프레임워크의 핵심 데이터 모델  
lazy val specCore \= (project in file("spec-core"))  
  .settings(  
    name := "spec-core"  
  )

// spec-macros: @LocalSpec 매크로 구현  
lazy val specMacros \= (project in file("spec-macros"))  
  .settings(  
    name := "spec-macros"  
  )

// spec-plugin: SBT 빌드 태스크 정의 (SBT 플러그인)  
lazy val specPlugin \= (project in file("spec-plugin"))  
  .settings(  
    name := "spec-plugin"  
  )

// design: 사용자 RTL 디자인 코드 및 스펙 정의  
// (수정됨): design 프로젝트는 이제 루트 빌드에 의해 집계되지 않습니다.  
// 이는 design이 완전히 독립적인 별도의 빌드 단위로 관리됨을 의미합니다.  
lazy val design \= (project in file("design"))  
  .settings(  
    name := "design"  
  )

// \---------- Root Project Aggregation \----------  
lazy val root \= (project in file("."))  
  // (수정됨): design과 specPlugin 프로젝트를 aggregate 대상에서 제외합니다.  
  // 이들은 이제 독립적인 빌드 단위로 관리되거나, 다른 프로젝트에 의해 명시적으로 사용됩니다.  
  .aggregate(specCore, specMacros) // 루트는 이제 핵심 라이브러리만 집계합니다.  
  .settings(  
    publish / skip := true // 루트 프로젝트는 배포하지 않음  
  )

**2\. project-root/project/plugins.sbt**

// project/plugins.sbt

// Scala 2.13부터는 매크로 어노테이션이 기본 지원되므로 sbt-paradise 플러그인은 더 이상 필요 없습니다.  
// addSbtPlugin("org.scalamacros" % "sbt-paradise" % "2.1.1" cross CrossVersion.full)

// (수정됨): spec-plugin에 대한 addSbtPlugin 라인을 제거합니다.  
// spec-plugin은 이제 design 프로젝트의 로컬 plugins.sbt에서만 로드됩니다.  
// addSbtPlugin("your.company" % "spec-plugin" % "0.1.0-SNAPSHOT")

// (선택 사항) SBT 빌드 자체에 필요한 다른 전역 플러그인들은 여기에 남을 수 있습니다.  
// 예를 들어, buildinfo나 의존성 그래프 플러그인 등  
// addSbtPlugin("org.scala-sbt" % "sbt-plugin" % "1.10.0") // 이 또한 일반적으로 불필요

**3\. project-root/spec-core/build.sbt**

// spec-core/build.sbt

// 프로젝트 이름 설정  
name := "spec-core"

// 라이브러리 의존성 추가  
libraryDependencies \++= Seq(  
  "com.lihaoyi" %% "upickle" % "2.0.0", // JSON 직렬화를 위함  
  "org.scala-lang" % "scala-reflect" % scalaVersion.value // 매크로를 위함 (spec-macros에서 의존)  
)

// (\!\!\!중요\!\!\!): 이 프로젝트 자체를 Scala 2.13.12와 2.12.19 두 가지 버전으로 크로스 컴파일하도록 명시합니다.  
// 이렇게 해야 spec-core\_2.12 아티팩트가 생성되어 spec-plugin이 이를 찾을 수 있습니다.  
crossScalaVersions := Seq("2.13.12", "2.12.19")

// Scala 2.13에서만 \-Ymacro-annotations 컴파일러 옵션을 추가합니다.  
// \`=\` 연산자를 사용하여 상속된 옵션을 완전히 덮어씁니다.  
Compile / compile / scalacOptions := {  
  CrossVersion.partialVersion(scalaVersion.value) match {  
    case Some((2, 13)) \=\> Seq("-Ymacro-annotations")  
    case \_ \=\> Seq() // Scala 2.12에서는 옵션을 추가하지 않음  
  }  
}

// Scaladoc(문서화) 시에도 \-Ymacro-annotations 옵션이 Scala 2.13에서만 추가되도록 합니다.  
// 이를 통해 Scala 2.12에서 문서 생성 시 발생하는 오류를 방지합니다.  
Compile / doc / scalacOptions := {  
  CrossVersion.partialVersion(scalaVersion.value) match {  
    case Some((2, 13)) \=\> Seq("-Ymacro-annotations")  
    case \_ \=\> Seq() // Scala 2.12에서는 옵션을 추가하지 않음  
  }  
}

**4\. project-root/spec-macros/build.sbt**

// spec-macros/build.sbt

// 프로젝트 이름 설정  
name := "spec-macros"

// (\!\!\!중요\!\!\!): specCore 프로젝트에 대한 의존성을 'dependsOn(specCore)' 대신 라이브러리 의존성으로 변경합니다.  
// 이렇게 하면 spec-macros는 specCore가 로컬에 publishLocal된 아티팩트를 참조하게 됩니다.  
// '%%'는 현재 Scala 버전(2.13.12)에 맞는 spec-core 아티팩트(spec-core\_2.13)를 찾도록 합니다.  
libraryDependencies \+= "your.company" %% "spec-core" % "0.1.0-SNAPSHOT"

// 라이브러리 의존성 추가  
libraryDependencies \+= "org.scala-lang" % "scala-reflect" % scalaVersion.value

// 매크로 어노테이션을 활성화하는 컴파일러 옵션  
Compile / scalacOptions \+= "-Ymacro-annotations"

// spec-macros도 크로스 컴파일 버전이 명시되어야 합니다.  
// 현재는 2.13만 사용하므로 2.13만 명시. 향후 Scala 3 추가 시 \`Seq("2.13.12", "3.x.x")\`로 변경.  
crossScalaVersions := Seq("2.13.12")

**5\. project-root/spec-plugin/build.sbt**

// spec-plugin/build.sbt

// 이 프로젝트가 SBT 플러그임을 명시합니다.  
sbtPlugin := true

// (\!\!\!중요\!\!\!): 플러그인 프로젝트의 Scala 버전을 명시적으로 설정합니다.  
// SBT 런타임은 일반적으로 Scala 2.12.x 기반으로 빌드되므로, 여기에 맞춰야 합니다.  
scalaVersion := "2.12.19"

// (\!\!\!중요\!\!\!): spec-core에 대한 의존성을 라이브러리 의존성으로 변경합니다.  
// \`%%\`를 사용하여 현재 Scala 버전(2.12.19)에 맞는 spec-core 아티팩트(spec-core\_2.12)를 찾습니다.  
// spec-core는 이제 crossScalaVersions 설정 덕분에 2.12 버전 아티팩트를 publishLocal 합니다.  
libraryDependencies \+= "your.company" %% "spec-core" % "0.1.0-SNAPSHOT"

// enablePlugins(SbtPlugin)은 여기서 선언합니다.  
enablePlugins(SbtPlugin)

// 플러그인 자체에 매크로 어노테이션이 사용되지 않는다면, 이 옵션은 불필요하므로 제거합니다.  
// Compile / scalacOptions \+= "-Ymacro-annotations"

**6\. project-root/design/build.sbt (사용자 프로젝트)**

// design/build.sbt

// 프로젝트 이름 설정  
name := "design"

// (수정됨): specCore 및 specMacros에 대한 의존성을 libraryDependencies로 변경합니다.  
// 이렇게 하면 design 프로젝트는 specCore와 specMacros가 로컬에 publishLocal된 아티팩트를 참조하게 됩니다.  
libraryDependencies \++= Seq(  
  "your.company" %% "spec-core" % "0.1.0-SNAPSHOT",  
  "your.company" %% "spec-macros" % "0.1.0-SNAPSHOT"  
)

// Chisel 라이브러리 의존성 추가 (사용자 프로젝트의 핵심)  
libraryDependencies \+= "edu.berkeley.cs" %% "chisel3" % "3.5.6"

// 매크로 어노테이션을 활성화하는 컴파일러 옵션  
// design 프로젝트는 ThisBuild / scalaVersion에 의해 Scala 2.13으로만 컴파일되므로,  
// 조건부 로직 없이 이 옵션을 직접 추가해도 안전하고 더 명확합니다.  
Compile / scalacOptions := Seq("-Ymacro-annotations")

// spec-plugin의 태스크를 활성화합니다.  
// design/project/plugins.sbt에 addSbtPlugin("your.company" % "spec-plugin" % "0.1.0-SNAPSHOT")이 있으므로  
// 이 프로젝트에서 명시적으로 플러그인을 활성화할 수 있습니다.  
enablePlugins(SpecPlugin) // SpecPlugin을 활성화하여 exportSpecIndex 등의 태스크를 사용할 수 있게 함

**7\. project-root/design/project/plugins.sbt (새로 추가될 파일)**

// design/project/plugins.sbt

// design 프로젝트만 사용할 spec-plugin을 여기에 추가합니다.  
// 이렇게 하면 spec-plugin이 design 프로젝트의 로컬 스코프에서 로드됩니다.  
addSbtPlugin("your.company" % "spec-plugin" % "0.1.0-SNAPSHOT")

// (선택 사항) SBT 플러그인 개발을 위한 핵심 API 플러그인 (필요한 경우)  
// addSbtPlugin("org.scala-sbt" % "sbt-plugin" % "1.10.0")

## **6\. 구현 시 고려사항 및 확장 계획**

### **6.1. FIRRTL Transform (SpecPathTransform \- 향후 구현)**

* **핵심 과제:** 현재 MVP에서는 Tag의 fullyQualifiedModuleName과 hardwareInstancePath가 플레이스홀더로 남아있습니다. 이 필드들은 FIRRTL 컴파일 과정에서 해당 태그가 연결된 실제 하드웨어 인스턴스 경로와 소스 코드상의 완전한 모듈 이름을 정확히 파악하여 채워져야 합니다.  
* **구현 방법:** Chisel의 FIRRTL 컴파일러 플러그인 메커니즘을 활용하여 커스텀 FIRRTL Transform을 구현해야 합니다. 이 Transform은 FIRRTL IR의 Module 및 Instance 노드를 순회하며 실제 계층 경로를 파악하고, 이를 SpecRegistry.Tag (또는 FIRRTL Annotation 자체)에 다시 주입하는 방식으로 작동합니다.  
* **난이도:** FIRRTL 내부 구조 및 Transform API에 대한 깊은 이해가 필요하여 프레임워크 개발에서 가장 복잡한 부분 중 하나입니다.

### **6.2. definitionFile 자동 채우기 (향후 개선)**

* HardwareSpecification의 definitionFile 필드를 스펙이 정의된 Scala 파일 경로로 자동 채우는 것은 Spec.build() 내부에서 현재 파일 경로를 캡처하는 매크로(또는 컴파일러 플러그인)를 통해 구현할 수 있습니다. 이는 MVP 이후의 개선 과제로 남겨둡니다.

### **6.3. specLint 규칙 상세 구현 (향후 구현)**

* Lint 규칙(NoContractPerModule, UnusedSpec 등)은 SpecIndex.json을 파싱하고, 그 안의 데이터를 분석하여 정의된 기준에 따라 경고나 오류를 발생시키는 로직으로 구현됩니다. 각 규칙은 별도의 함수나 클래스로 모듈화하여 관리하는 것이 좋습니다.

### **6.4. reportGen 기능 고도화 (향후 구현)**

* HTML/PDF 보고서 생성은 SpecIndex.json과 TagIndex.json을 시각적으로 매력적이고 유용한 대시보드 형태로 렌더링하는 작업입니다. 이는 웹 프레임워크(예: Scalatags, Scala.js 기반의 프런트엔드)나 보고서 생성 라이브러리를 활용할 수 있습니다.
* 외부 검증 로그(verifications.csv)와의 통합은 CSV 파싱 및 스펙 ID와의 매핑 로직을 포함합니다.

### **6.5. Scala 3 마이그레이션 로드맵 (장기)**

현재 Scala 2.13.12를 사용하지만, 장기적으로 Scala 3으로의 마이그레이션을 고려할 수 있습니다.

* **주요 변경점**: Scala 3은 scala-reflect 매크로를 대체하는 새로운 메타프로그래밍 API (scala.quoted)를 제공합니다. 이는 매크로 개발을 더 직관적이고 타입 안전하게 만들지만, 기존 scala-reflect 코드를 상당 부분 재작성해야 합니다. scala.quoted를 사용하면 @LocalSpec(MyExampleSpecs.QueueSpec)와 같이 HardwareSpecification 객체를 직접 매크로 인자로 전달하는 초기 설계를 더 안정적으로 구현할 수 있게 됩니다.  
* **마이그레이션 전략**: spec-macros 프로젝트 내부에 src/main/scala-2.13/와 src/main/scala-3/와 같이 별도의 소스 디렉토리를 만들어, 각 Scala 버전에 맞는 LocalSpec.scala 구현을 분리하여 관리할 수 있습니다.  
* **이점**: 더 강력하고 안정적인 DSL 구현 가능성, 개선된 컴파일 시간 성능, 새로운 언어 기능 활용.

## **7\. 문제 해결 (Troubleshooting) \- SBT 관련 일반적인 문제**

SBT 빌드 시스템은 강력하지만, 특히 플러그인 개발이나 복잡한 멀티-프로젝트 설정 시 예상치 못한 문제를 야기할 수 있습니다. 다음은 이 프레임워크 개발 과정에서 발생할 수 있거나 일반적으로 흔한 SBT 문제와 해결 방법입니다.

### **7.1. scripted-sbt 의존성을 찾을 수 없다는 오류**

org.scala-sbt:scripted-sbt\_2.13:1.10.0와 같은 의존성을 찾을 수 없다는 오류는 플러그인 프로젝트를 빌드할 때 나타날 수 있습니다. scripted-sbt는 SBT 플러그인 테스트에 사용되는 내부 컴포넌트입니다.

* **원인:**  
  1. **불필요한 명시적 sbt-plugin 의존성:** project/plugins.sbt에 addSbtPlugin("org.scala-sbt" % "sbt-plugin" % "1.10.0")와 같이 SBT 자체에 내장된 핵심 플러그인 API를 명시적으로 추가하면 SBT 내부 의존성 관리와 충돌하여 이런 문제가 발생할 수 있습니다. **이 라인은 제거해야 합니다.** (루트 project/plugins.sbt에서 이미 제거됨)  
  2. **플러그인 프로젝트의 잘못된 aggregate:** 루트 build.sbt의 aggregate에 specPlugin과 같은 플러그인 프로젝트를 포함하면, SBT가 플러그인 프로젝트를 일반 서브 프로젝트처럼 다루려 하면서 플러그인 개발과 관련된 특정 의존성(예: scripted)을 비정상적으로 찾을 수 있습니다. (루트 build.sbt에서 specPlugin이 aggregate에서 제외됨)  
  3. **crossScalaVersions 불일치:** SBT 플러그인은 SBT 런타임이 사용하는 Scala 버전에 맞춰 컴파일되어야 합니다. spec-plugin/build.sbt에 scalaVersion을 SBT 런타임 버전(2.12.19)에 맞춰 명시적으로 설정해야 합니다.  
* **해결 방법:**  
  1. **project/plugins.sbt에서 불필요한 addSbtPlugin("org.scala-sbt" % "sbt-plugin" % "1.10.0") 라인 제거.** (이미 완료됨)  
  2. **루트 build.sbt의 root 프로젝트 aggregate 목록에서 specPlugin 제거.** (이미 완료됨)  
  3. **spec-plugin 프로젝트에 scalaVersion 명시적으로 설정:** spec-plugin/build.sbt에 scalaVersion := "2.12.19" (또는 현재 사용 중인 SBT 1.x 버전에 맞는 Scala 2.12.x 버전)를 추가하여 플러그인이 SBT와 호환되는 Scala 버전으로 컴파일되도록 합니다. (이미 완료됨)  
  4. **SBT 캐시 삭제 후 재시도:** 이전의 잘못된 의존성 정보가 남아있을 수 있으므로, 다음 명령을 실행하여 캐시를 완전히 삭제하고 빌드를 재시도합니다.  
     sbt clean  
     rm \-rf \~/.ivy2/cache/org.scala-sbt \~/.sbt/boot/  
     sbt compile

### **7.2. 매크로 확장 시 ToolBoxError: reflective compilation has failed: object X is not a member of package Y**

@LocalSpec 어노테이션 사용 시, 매크로 확장 중에 ToolBoxError가 발생하며 "object X is not a member of package Y"와 같은 메시지가 나타날 수 있습니다.

* **원인:**  
  * **c.eval()의 제약:** Scala 2의 scala-reflect 매크로 내부의 c.eval()은 현재 컴파일 중인 프로젝트(예: design 프로젝트) 내에 정의된 **복합적인 val (예: MyExampleSpecs.QueueSpec)을 컴파일 시점에 평가하려고 시도할 때 이 오류를 발생시킵니다.** 매크로가 실행되는 시점에는 해당 val의 최종 바이트코드나 완전히 확정된 값이 아직 매크로의 내부 컴파일러(ToolBox)가 접근할 수 있는 형태로 준비되지 않았기 때문입니다.  
  * 이 오류는 매크로가 HardwareSpecification 객체 자체를 인자로 받을 때 발생하며, 빌더 패턴 사용 여부와는 무관합니다.  
* **해결 방법 (현재 Scala 2.13 환경에서 최선):**  
  * \*\*@LocalSpec 어노테이션에 HardwareSpecification 객체 대신 **스펙의 고유 ID를 문자열 리터럴로 직접 전달**하도록 변경합니다.  
    * 예: @LocalSpec("QUEUE\_FUNC\_001")  
  * LocalSpec.scala 매크로 구현에서 c.eval()을 사용하여 HardwareSpecification 객체를 평가하는 로직을 제거하고, 대신 문자열 리터럴 인자를 직접 추출하여 Tag.id 필드에 사용합니다.  
  * **SBT 캐시를 완전히 정리하고 재컴파일**해야 합니다. (섹션 7.1의 SBT 캐시 삭제 명령 참조)

### **7.3. 매크로 관련 implicit not found 또는 확장 오류**

일반적인 매크로 어노테이션 (@LocalSpec) 사용 시 implicit not found 오류나 매크로 확장 실패 오류가 발생할 수 있습니다.

* **원인:**  
  1. **scalacOptions \+= "-Ymacro-annotations" 누락:** 매크로 어노테이션을 활성화하는 컴파일러 옵션이 적용되지 않은 경우.  
  2. **scala-reflect 의존성 누락:** 매크로 구현에 필요한 scala-reflect 라이브러리가 프로젝트에 추가되지 않은 경우.  
  3. **잘못된 upickle ReadWriter 정의:** SpecCategory와 같이 sealed trait와 case object가 혼합된 ADT에 대해 upickle.default.macroRW만 사용하면 직렬화/역직렬화가 실패할 수 있습니다. (이 문제는 SpecCategory의 명시적 Reader/Writer 구현으로 해결됨)  
* **해결 방법:**  
  1. **모든 관련 프로젝트(spec-core, spec-macros, design)의 build.sbt에 Compile / scalacOptions \+= "-Ymacro-annotations"가 있는지 확인.** (이제 design/build.sbt와 spec-core/build.sbt에서는 조건부 로직을 사용하여 2.13에서만 활성화됩니다.)  
  2. **spec-core와 spec-macros의 build.sbt에 libraryDependencies \+= "org.scala-lang" % "scala-reflect" % scalaVersion.value가 있는지 확인.**  
  3. **SpecCategory의 implicit val rw: ReadWriter\[SpecCategory\] 정의가 명시적인 Reader와 Writer를 사용하여 모든 case object 및 case class를 문자열로 매핑하는지 확인.** (섹션 3.1의 SpecCategory 코드 참고)

### **7.4. design/target/SpecIndex.json 및 TagIndex.json이 비어 있거나 올바르지 않게 생성됨**

JSON 파일에 스펙 정보가 없거나, 내용이 불완전하거나, 형식이 맞지 않는 경우입니다.

* **원인:**  
  1. **spec.meta.dir 시스템 속성 누락 또는 MetaFile.setBaseDir 호출 누락:** MetaFile이 파일을 쓸 대상 디렉토리를 알지 못하는 경우.  
  2. **Spec.build() 호출 누락:** 스펙 정의 파일에서 각 스펙 빌더 체이닝의 마지막에 .build() 메서드가 호출되지 않은 경우.  
  3. **SpecRegistry에 스펙/태그가 등록되지 않음:** 매크로나 빌더가 제대로 작동하지 않아 SpecRegistry.addSpec() 또는 SpecRegistry.addTag()가 호출되지 않은 경우.  
  4. **SpecPlugin.scala의 JSON 생성 로직 오류:** SpecIndexEntry 또는 TagIndex.json의 JSON 직렬화 로직에 문제가 있는 경우.
  5. **SBT 캐시 문제:** 이전 빌드의 잔여물이나 잘못된 의존성 정보가 남아있어 올바른 코드가 컴파일되지 않는 경우.  
* **해결 방법:**  
  1. **design/build.sbt에 Compile / resourceGenerators 태스크 내에서 \_root\_.framework.spec.MetaFile.setBaseDir((Compile / resourceManaged).value / "spec-meta").toPath)를 명시적으로 호출하는지 확인.**  
  2. **모든 스펙 정의 (MyExampleSpecs.scala 등)의 마지막에 .build()가 호출되었는지 확인.**  
  3. **SpecRegistry.addSpec 및 SpecRegistry.addTag가 실행되는지 확인:** MetaFile.scala 및 LocalSpec.scala에 추가된 println (DEBUG 로그)을 통해 컴파일 시점에 이들이 호출되는지 확인합니다.  
  4. **spec-plugin/src/main/scala/spec/plugin/SpecPlugin.scala 파일이 제가 제공한 최신 버전으로 정확히 일치하는지 확인합니다.** 특히 TagIndex.json을 맵 형태로 생성하는 로직과 SpecIndexEntry의 필드 매핑 로직을 주의 깊게 살펴봅니다.
  5. **가장 강력한 SBT 캐시 정리 수행 후 재시도.** (섹션 7.1의 SBT 캐시 삭제 명령 참조)

### **7.5. scala.jdk.CollectionConverters.\_ vs scala.collection.JavaConverters.\_ 오류**

Scala 버전에 따라 컬렉션 변환 임포트가 달라 발생하는 오류입니다.

* **원인:**  
  * Scala 2.13부터는 scala.jdk.CollectionConverters를 사용하고, Scala 2.12 이하에서는 scala.collection.JavaConverters를 사용해야 합니다.  
  * spec-core 프로젝트처럼 여러 Scala 버전으로 크로스 컴파일되는 모듈에 공통 소스 파일(src/main/scala/)에 이 임포트가 포함될 경우 문제가 발생합니다.  
* **해결 방법:**  
  * **해당 파일을 Scala 버전별 소스 디렉토리(src/main/scala-2.12/ 및 src/main/scala-2.13/)로 분리**하고, 각 파일에서 해당 Scala 버전에 맞는 임포트 (scala.collection.JavaConverters.\_ 또는 scala.jdk.CollectionConverters.\_)를 사용합니다.  
  * (이 문제는 이미 SpecBuilder.scala를 Scala 버전별로 분리하면서 해결되었습니다. SpecRegistry.scala도 마찬가지입니다.)

### **7.6. publish.sh 실행 시 unresolved dependency 또는 not found 오류**

라이브러리 아티팩트를 찾을 수 없다는 오류입니다.

* **원인:**  
  1. **publishLocal 실패:** 이전 단계의 프로젝트(예: spec-core)가 publishLocal에 실패하여 다음 프로젝트(예: spec-macros)가 의존하는 아티팩트를 로컬 Ivy 저장소에서 찾을 수 없는 경우.  
  2. **build.sbt의 의존성 선언 오류:** dependsOn과 libraryDependencies의 혼용 또는 잘못된 사용. SBT 멀티-프로젝트에서는 서브 프로젝트 간의 의존성은 dependsOn을 사용하고, 외부 라이브러리나 publishLocal된 아티팩트에 대한 의존성은 libraryDependencies를 사용해야 합니다. 특히 플러그인 프로젝트(spec-plugin)나 매크로 프로젝트(spec-macros)는 루트의 dependsOn 대신 libraryDependencies로 spec-core를 참조해야 합니다.  
  3. **scalaVersion 또는 crossScalaVersions 불일치:** libraryDependencies에서 %%를 사용할 때, 해당 프로젝트의 scalaVersion과 의존하는 아티팩트의 빌드 scalaVersion이 일치하지 않으면 아티팩트를 찾을 수 없습니다. (예: spec-plugin이 Scala 2.12로 빌드되는데 spec-core\_2.13을 찾으려 하는 경우)  
  4. **SBT 캐시 문제:** 오래된 캐시 정보가 의존성 해결을 방해하는 경우.  
* **해결 방법:**  
  1. **publish.sh 스크립트의 실행 순서가 올바른지 확인:** spec-core \-\> spec-macros \-\> spec-plugin 순서로 publishLocal이 성공해야 합니다.  
  2. **모든 build.sbt 파일의 의존성 선언을 최종 버전으로 일치시키고, dependsOn과 libraryDependencies를 올바르게 사용했는지 확인.** (섹션 5.2의 build.sbt 코드 참고)  
     * spec-core의 crossScalaVersions에 2.12.19와 2.13.12가 모두 포함되어 spec-core\_2.12와 spec-core\_2.13 아티팩트가 모두 발행되는지 확인.  
     * spec-plugin의 scalaVersion이 2.12.19로 고정되어 있고, libraryDependencies \+= "your.company" %% "spec-core" % "0.1.0-SNAPSHOT"를 통해 spec-core\_2.12를 참조하는지 확인.  
     * spec-macros의 crossScalaVersions에 2.13.12가 포함되어 있고, libraryDependencies \+= "your.company" %% "spec-core" % "0.1.0-SNAPSHOT"를 통해 spec-core\_2.13을 참조하는지 확인.  
  3. **강력한 SBT 캐시 정리 후 publish.sh 재실행.** (섹션 7.1의 SBT 캐시 삭제 명령 참조)