# 하드웨어 스펙-태그 관리 프레임워크(Spec Framework) 기술 문서

## 1. 목적

하드웨어 스펙과 이를 사용하는 RTL 모듈 간의 명확하고 정확한 연결 관계를 컴파일 타임에 확보하여 다음 목표를 달성한다:

* 스펙 문서화 정확성 및 추적 가능성 극대화
* 사용자 편의성과 유지보수성 보장
* IDE 및 자동화 도구의 정확한 정적 분석 지원

## 2. 핵심 개념

### 2.1 Spec

* 사용자가 정의한 하드웨어 스펙 객체 (`HardwareSpecification`)
* Spec DSL을 통해 정의:

```scala
val contractPacketFilter = emitSpec {
  CONTRACT("CONTRACT_PACKET_FILTER", "Packet filtering description...")
    .capability(Capability("PacketFilter"))
    .build()
}
```

### 2.2 Spec ID

* 사용자가 명시적으로 입력하는 사람 친화적 ID (`String`)
* 예시: `CONTRACT_PACKET_FILTER`

### 2.3 Unique ID (Symbol 기반 FQN)

* Scala 컴파일러에서 추출한 스펙 정의의 정규화된 고유 심볼 이름
* 예시: `klase32.frontend.specs.modules.PacketFilterSpec.contractPacketFilter`

### 2.4 Pretty ID

* 사람이 보기 편한 식별자, `ObjectName.SpecId` 형태
* 예시: `PacketFilterSpec.CONTRACT_PACKET_FILTER`

### 2.5 RawTag

스펙과 RTL 모듈 간 연결을 나타내는 구조체:

* `specId`: 사용자 지정 Pretty ID
* `specSymbolFQN`: 심볼 기반 고유 ID
* `specPosKey`: 스펙 정의 파일 위치
* `moduleFQN`: RTL 모듈의 심볼 기반 위치

### 2.6 @SpecMeta 어노테이션

* 매크로가 컴파일 타임에 자동으로 스펙 객체에 부착하는 어노테이션
* 목적:

  * 컴파일 타임에 Spec의 식별자(specId)를 제공하여 매크로 및 IDE가 Spec과 관련된 정보를 쉽게 추적할 수 있게 함
  * Spec 정의를 사용하는 곳에서 명시적으로 연관된 Spec을 확인할 수 있게 도움

## 3. 컴포넌트 아키텍처

### 3.1 emitSpec 매크로

* 사용자의 DSL 표현식을 분석해 Spec 객체를 생성하고 JSON으로 기록
* 컴파일 타임에 `@SpecMeta` 어노테이션 자동 부착

### 3.2 @LocalSpec 매크로

* RTL 모듈과 Spec 객체를 연결할 때 사용:

```scala
@LocalSpec(contractPacketFilter)
class PacketFilter extends Module { ... }
```

* RawTag를 생성하여 TagRegistry에 등록

### 3.3 TagRegistry

* 컴파일 타임에 모든 RawTag 객체를 메모리 내 보관
* sbt 플러그인을 통해 JSON으로 출력

### 3.4 sbt Plugin (`spec-plugin`)

* 컴파일 종료 시점에 TagRegistry의 RawTag 데이터를 JSON 파일로 저장
* SpecRegistry 및 TagRegistry 데이터를 결합해 추적 인덱스 생성

### 3.5 후처리 도구

* 생성된 RawTag JSON 데이터를 이용해 HTML/Markdown 문서 생성 및 IDE 점프/추적 지원

## 4. 전체 프로그램 동작 시나리오

* 사용자가 Spec을 정의할 때 emitSpec 매크로를 이용하여 Spec 객체를 생성한다.
* emitSpec 매크로가 Spec 객체에 @SpecMeta 어노테이션을 자동으로 붙이고, 이를 JSON으로 기록한다.
* 사용자가 RTL 모듈을 구현할 때 @LocalSpec 매크로를 사용하여 해당 모듈과 Spec을 명시적으로 연결한다.
* @LocalSpec 매크로가 RawTag를 생성하여 TagRegistry에 등록한다.
* 컴파일 과정이 종료되면, sbt Plugin(spec-plugin)이 TagRegistry의 데이터를 JSON으로 저장하고 추적 인덱스를 만든다.
* 후처리 도구는 생성된 JSON 데이터를 사용하여 스펙과 모듈 간의 연결을 시각화하고, IDE에서의 추적 및 점프 기능을 제공한다.

## 5. JSON 데이터 구조

### 5.1 Spec JSON (스펙 정의)

Flat 구조이며, Unique ID를 키로 사용하여 관리:

```json
{
  "klase32.frontend.specs.modules.PacketFilterSpec.contractPacketFilter": {
    "prettyId": "PacketFilterSpec.CONTRACT_PACKET_FILTER",
    "specId": "CONTRACT_PACKET_FILTER",
    "specPosKey": "src/specs/modules/PacketFilterSpec.scala:42",
    "entries": { ... }
  }
}
```

### 5.2 Tag JSON (스펙-모듈 연결)

배열 구조를 통해 다:다 관계 명시적 표현:

```json
[
  {
    "specSymbolFQN": "klase32.frontend.specs.modules.PacketFilterSpec.contractPacketFilter",
    "moduleFQN": "klase32.frontend.modules.PacketFilter",
    "specId": "CONTRACT_PACKET_FILTER",
    "specPosKey": "src/specs/modules/PacketFilterSpec.scala:42"
  },
  {
    "specSymbolFQN": "klase32.frontend.specs.modules.PacketFilterSpec.contractPacketFilter",
    "moduleFQN": "klase32.frontend.modules.SomeOtherModule",
    "specId": "CONTRACT_PACKET_FILTER",
    "specPosKey": "src/specs/modules/PacketFilterSpec.scala:42"
  }
]
```

## 6. 권장 규칙

* Spec 정의 파일 및 object 이름은 Spec과 동일하게 일관성 유지
* Spec ID는 `{CATEGORY축약어}_{VAL이름대문자}` 형태 권장 (위반 시 컴파일 타임 경고 제공)
* Pretty ID는 편의성을 위해 현행 방식을 유지하며, 후처리 시 필요에 따라 변경 가능

## 7. 향후 고려 사항

* Incremental 컴파일 및 Scala 3 매크로 (`inline`, `quotes`) 전환 시 데이터 일관성 유지 전략
* 다중 컴파일 환경에서 RawTag 데이터 동시성 관리
* JSON 데이터의 자동화된 후처리 및 IDE/LSP 연동

이 기술 문서는 하드웨어 스펙-태그 관리 프레임워크의 설계 의도와 구현 방식을 명확히 제시하며, 후속 개발자나 새로운 세션이 바로 이해하고 이어나갈 수 있도록 구성되었습니다.
