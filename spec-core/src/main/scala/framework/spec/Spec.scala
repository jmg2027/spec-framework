// spec-core/src/main/scala/framework/spec/Spec.scala

package framework.spec

// upickle은 MetaFile에서만 필요하므로 여기서 직접 임포트하지 않습니다.
// java.io.File, java.nio.file.Files 등도 MetaFile에서 처리합니다.

/**
 * 하드웨어 스펙 정의를 위한 타입 안전하고 간결한 DSL (Domain Specific Language)입니다.
 * 이 DSL은 staged builder 패턴을 사용하여 스펙 생성 과정을 단계별로 강제하고,
 * 불변성(immutability)을 유지하며, 스펙이 완전히 정의되었을 때만 [[HardwareSpecification]]을 반환합니다.
 */
object Spec {

  // --- Entry-point functions ---
  // 이 함수들은 스펙 정의의 시작점이며, Stage1 빌더 인스턴스를 반환하여 다음 단계를 안내합니다.
  // 각 스펙 카테고리별로 팩토리 메서드를 제공합니다.

  def CONTRACT(id: String, desc: String): Stage1 =
    new Stage1(SpecCategory.CONTRACT, id, desc)

  def FUNCTION(id: String, desc: String): Stage1 =
    new Stage1(SpecCategory.FUNCTION, id, desc)

  def PROPERTY(id: String, desc: String): Stage1 =
    new Stage1(SpecCategory.PROPERTY, id, desc)

  def COVERAGE(id: String, desc: String): Stage1 =
    new Stage1(SpecCategory.COVERAGE, id, desc)

  def INTERFACE(id: String, desc: String): Stage1 =
    new Stage1(SpecCategory.INTERFACE, id, desc)

  def PARAMETER(id: String, desc: String): Stage1 =
    new Stage1(SpecCategory.PARAMETER, id, desc)

  def RAW(id: String, desc: String, prefix: String): Stage1 =
    new Stage1(SpecCategory.RAW(prefix), id, desc)


  // --- Staged builder types ---
  // 이 내부 클래스들은 스펙 빌더의 각 단계를 나타냅니다.
  // 각 메서드는 새로운 빌더 인스턴스를 반환하여 불변성을 유지합니다.

  /**
   * Stage1: `id`, `description`, `category`와 같은 필수 필드가 존재함을 보장합니다.
   * 이 단계에서는 `capability` 설정을 강제하거나 건너뛸 수 있습니다.
   * @param cat 스펙 카테고리
   * @param id 스펙 ID
   * @param desc 스펙 설명
   */
  final class Stage1 private[Spec] (
    cat:  SpecCategory,
    id:   String,
    desc: String
  ) {
    // 핵심 스펙 데이터를 저장하는 불변 Core 객체를 생성합니다.
    private def initialCore = Core(id, cat, desc)

    /** 스펙에 기능을 명시적으로 추가하고 다음 빌더 단계인 Stage2를 반환합니다. */
    def capability(c: Capability): Stage2 = new Stage2(initialCore.copy(capability = Some(c)))

    /** 스펙에 기능이 없음을 명시하고 다음 빌더 단계인 Stage2를 반환합니다. */
    def noCapability: Stage2 = new Stage2(initialCore)
  }

  /**
   * Stage2: 선택적이고 반복될 수 있는 필드들을 누적합니다.
   * 모든 메서드는 새로운 Stage2 인스턴스를 반환하여 불변성을 유지합니다.
   * 마지막으로 `build()` 메서드를 통해 최종 스펙 객체를 생성하고 사이드 이펙트(파일 출력 등)를 발생시킵니다.
   * @param core 현재까지 누적된 스펙 데이터를 담는 불변 Core 객체
   */
  final class Stage2 private[Spec] (core: Core) {

    // --- Optional & Repeated Fields ---
    // 단수/복수 인자를 varargs로 통합하여 API를 간소화합니다.
    // 각 메서드는 새로운 Core 인스턴스를 가진 새로운 Stage2를 반환합니다.

    /** 부모 스펙 ID들을 추가합니다. varargs를 사용하여 여러 ID를 한 번에 추가할 수 있습니다. */
    def parents(ids: String*): Stage2 = copy(core.copy(parentIds = core.parentIds ++ ids))

    /** 관련 스펙 ID들을 추가합니다. varargs를 사용하여 여러 ID를 한 번에 추가할 수 있습니다. */
    def related(ids: String*): Stage2 = copy(core.copy(relatedToIds = core.relatedToIds ++ ids))

    /** 스펙의 상태를 설정합니다. */
    def status(s: String): Stage2 = copy(core.copy(status = Some(s)))

    /** 스펙 구현자를 설정합니다. */
    def impl(by: String): Stage2 = copy(core.copy(implementedBy = Some(by)))

    /** 스펙 검증자를 설정합니다. */
    def verified(by: String): Stage2 = copy(core.copy(verifiedBy = Some(by)))

    /** 스펙이 요구하는 Capability ID들을 추가합니다. varargs를 사용하여 여러 ID를 한 번에 추가할 수 있습니다. */
    def requiresCaps(ids: String*): Stage2 = copy(core.copy(requiredCaps = core.requiredCaps ++ ids))

    /** 추가 메타데이터 키-값 쌍을 추가합니다. varargs를 사용하여 여러 쌍을 한 번에 추가할 수 있습니다. */
    def meta(kv: (String,String)*): Stage2 = copy(core.copy(metadata = core.metadata ++ kv.toMap))

    /** 스펙 엔트리(name-value 쌍)를 추가합니다. */
    def entry(name: String, value: String): Stage2 =
      copy(core.copy(entries = core.entries :+ SpecEntry(name, value)))

    // --- TERMINAL ---
    /**
     * 빌더를 완료하고 최종 [[HardwareSpecification]] 객체를 생성합니다.
     * 이 메서드가 호출될 때 스펙의 유효성 검사 및 파일 출력과 같은 사이드 이펙트가 발생합니다.
     * @return 완전히 정의된 HardwareSpecification 객체
     */
    def build(): HardwareSpecification = {
      // 1. 유효성 검사 (compile-time safety를 넘어선 런타임 검증)
      // `require`를 사용하여 조건을 만족하지 않으면 IllegalArgumentException을 발생시킵니다.
      require(!core.id.contains(" "), s"Spec ID '${core.id}' must not contain spaces. Use URI-safe characters.")
      require(core.entries.nonEmpty, s"Spec '${core.id}' must define at least one entry.")
      // SpecRegistry.lookup을 사용하여 중복 ID를 검사하려면 SpecRegistry에 lookup 메서드가 필요합니다.
      // 현재 SpecRegistry는 addSpec만 있으므로, 이 부분은 SpecRegistry가 Query 가능하도록 확장되거나,
      // 플러그인 단계에서 최종적으로 검증되어야 합니다.
      // 일단 SpecRegistry.addSpec에서 중복시 경고/오류를 내보낼 수 있도록 합니다.

      // 2. 불변 Core 객체로부터 최종 HardwareSpecification 객체 생성
      val spec = core.toHardwareSpec

      // 3. 사이드 이펙트: 파일 출력 및 SpecRegistry 등록
      // 이 부분은 SpecBuilder의 apply()에서 가져왔습니다.
      MetaFile.writeSpec(spec)      // .spec 파일로 스펙 메타데이터 출력
      SpecRegistry.addSpec(spec)    // SpecRegistry에 스펙 객체 등록 (디버깅 및 플러그인 로딩 이전의 참조용)

      spec // 최종 HardwareSpecification 객체 반환
    }

    // 내부 헬퍼: 현재 Stage2의 Core 객체를 복사하여 새로운 Stage2 인스턴스를 만듭니다.
    private def copy(c: Core) = new Stage2(c)
  }

  // --- Internal immutable aggregator ---
  // 이 case class는 스펙의 모든 필드를 불변 상태로 집계하는 데 사용됩니다.
  // 빌더 단계에서 이 Core 객체의 복사본을 생성하여 필드를 변경합니다.
  private final case class Core(
    id: String,
    cat: SpecCategory,
    desc: String,
    capability:      Option[Capability]      = None,
    status:          Option[String]          = None,
    metadata:        Map[String,String]      = Map.empty,
    parentIds:       Set[String]             = Set.empty,
    relatedToIds:    Set[String]             = Set.empty, // (FIXED): _relatedToIds 대신 relatedToIds
    implementedBy:   Option[String]          = None,
    verifiedBy:      Option[String]          = None,
    requiredCaps:    Set[String]             = Set.empty, // (FIXED): requiredCapabilities 대신 requiredCaps
    definitionFile:  Option[String]          = None,
    entries:         List[SpecEntry]         = Nil
  ) {
    // Core 객체를 최종 HardwareSpecification 객체로 변환하는 메서드
    def toHardwareSpec: HardwareSpecification =
      HardwareSpecification(
        id = id,
        category = cat,
        description = desc,
        capability = capability,
        status = status,
        metadata = metadata,
        parentIds = parentIds,
        relatedToIds = relatedToIds, // (FIXED): _relatedToIds 대신 relatedToIds
        implementedBy = implementedBy,
        verifiedBy = verifiedBy,
        requiredCapabilities = requiredCaps, // (FIXED): requiredCapabilities 대신 requiredCaps
        definitionFile = definitionFile,
        entries = entries
      )
  }
}
