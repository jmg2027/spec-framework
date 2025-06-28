package framework.spec

/**
 * SpecBuilder: Builder-pattern DSL for creating HardwareSpecification objects.
 *
 * @param id        Unique spec ID
 * @param category  Spec category (CONTRACT, FUNCTION, etc.)
 * @param description Description of the spec
 *
 * Use chaining methods to set fields, then call apply() to create a HardwareSpecification
 * instance and automatically register it in the SpecRegistry.
 */
class SpecBuilder(val id: String, val category: SpecCategory, val description: String) {
  private var _capability: Option[Capability] = None
  private var _parentIds: Set[String] = Set.empty
  private var _metadata: Map[String, String] = Map.empty
  private var _status: Option[String] = None
  private var _relatedToIds: Set[String] = Set.empty
  private var _implementedBy: Option[String] = None
  private var _verifiedBy: Option[String] = None
  private var _requiredCapabilities: Set[String] = Set.empty

  /**
   * Set the capability this spec represents.
   * @param cap Capability object
   */
  def hasCapability(cap: Capability): SpecBuilder = { _capability = Some(cap); this }
  /**
   * Add a parent spec ID.
   */
  def parent(parentId: String): SpecBuilder = { _parentIds += parentId; this }
  /**
   * Add multiple parent spec IDs.
   */
  def parents(parentIds: String*): SpecBuilder = { _parentIds ++= parentIds; this }
  /**
   * Add metadata (key-value pair).
   */
  def withMetadata(key: String, value: String): SpecBuilder = { _metadata += (key -> value); this }
  /**
   * Set the spec status (e.g., DRAFT, APPROVED).
   */
  def withStatus(s: String): SpecBuilder = { _status = Some(s); this }
  /**
   * Add a related spec ID.
   */
  def relatedTo(relatedId: String): SpecBuilder = { _relatedToIds += relatedId; this }
  /**
   * Set the implementation code path.
   */
  def implementedBy(path: String): SpecBuilder = { _implementedBy = Some(path); this }
  /**
   * Set the verification code path.
   */
  def verifiedBy(path: String): SpecBuilder = { _verifiedBy = Some(path); this }
  /**
   * Set required capability ID set.
   */
  def requiredCapabilities(caps: Set[String]): SpecBuilder = { _requiredCapabilities = caps; this }

  /**
   * Create a HardwareSpecification instance from the builder and register it in SpecRegistry.
   * @return HardwareSpecification instance
   */
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
    SpecRegistry.addSpec(spec) // Register in SpecRegistry upon creation
    spec
  }
}

/**
 * Specs: Factory object for category-specific spec builders.
 *
 * Example: Specs.CONTRACT("ID", "desc") ... ()
 */
object Specs {
  def CONTRACT(id: String, description: String) = new SpecBuilder(id, SpecCategory.CONTRACT, description)
  def FUNCTION(id: String, description: String) = new SpecBuilder(id, SpecCategory.FUNCTION, description)
  def PROPERTY(id: String, description: String) = new SpecBuilder(id, SpecCategory.PROPERTY, description)
  def COVERAGE(id: String, description: String) = new SpecBuilder(id, SpecCategory.COVERAGE, description)
  def INTERFACE(id: String, description: String) = new SpecBuilder(id, SpecCategory.INTERFACE, description)
  def PARAMETER(id: String, description: String) = new SpecBuilder(id, SpecCategory.PARAMETER, description)
  def RAW(id: String, description: String, prefix: String) = new SpecBuilder(id, SpecCategory.RAW(prefix), description)
}
