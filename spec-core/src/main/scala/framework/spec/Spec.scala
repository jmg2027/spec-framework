// spec-core/src/main/scala/framework/spec/Spec.scala
// Type-safe staged DSL for hardware specification definition.
// Provides a builder pattern for constructing HardwareSpecification objects
// with compile-time and runtime validation, and macro-based metadata emission.
package framework.spec

/**
 * Spec: Type-safe and concise DSL (Domain Specific Language) for defining
 * hardware specifications.
 *
 *   - Uses a staged builder pattern to enforce stepwise, correct construction
 *     of specs.
 *   - Maintains immutability at every stage; each method returns a new builder
 *     instance.
 *   - Only returns a [[HardwareSpecification]] when the spec is fully defined
 *     and validated.
 *   - Triggers compile-time emission of .spec files and registration with
 *     SpecRegistry.
 *
 * Usage Example: val mySpec = Spec.CONTRACT("id",
 * "desc").capability(Capability("foo")).entry("k", "v").build()
 */
object Spec {

  // --------------------------------------------------------------------------
  // Entry-point functions: Factory methods for each spec category
  // These return a Stage1 builder instance to guide the next step in the DSL.
  // --------------------------------------------------------------------------

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

  // --------------------------------------------------------------------------
  // Staged builder types: Each stage enforces required/optional fields
  // Each method returns a new builder instance to maintain immutability.
  // --------------------------------------------------------------------------

  /**
   * Stage1: Ensures required fields like `id`, `description`, and `category`
   * exist. At this stage, the user must set or skip the capability field.
   * Returns Stage2 for further optional/repeatable fields.
   * @param cat
   *   Spec category (e.g., CONTRACT, FUNCTION, ...)
   * @param id
   *   Unique spec ID
   * @param desc
   *   Human-readable description
   */
  final class Stage1 private[Spec] (
    cat: SpecCategory,
    id: String,
    desc: String,
  ) {
    // Create an immutable Core object to store the core spec data for this builder instance.
    private def initialCore = Core(id, cat, desc)

    /**
     * Explicitly add a capability to the spec and return the next builder
     * stage, Stage2.
     */
    def capability(c: Capability): Stage2 = new Stage2(
      initialCore.copy(capability = Some(c)),
    )

    /**
     * Explicitly specify that the spec has no capability and return the next
     * builder stage, Stage2.
     */
    def noCapability: Stage2 = new Stage2(initialCore)
  }

  /**
   * Stage2: Accumulates optional and repeatable fields for the spec. All
   * methods return a new Stage2 instance to maintain immutability. The
   * `build()` method creates the final HardwareSpecification and triggers side
   * effects.
   * @param core
   *   Immutable Core object containing the accumulated spec data so far
   */
  final class Stage2 private[Spec] (core: Core) {

    // ----------------------------------------------------------------------
    // Optional & Repeated Fields: All methods return a new Stage2 with updated Core
    // Single/multiple arguments are unified with varargs for convenience.
    // ----------------------------------------------------------------------

    /** Add parent spec IDs. Supports multiple IDs at once via varargs. */
    def parents(ids: String*): Stage2 = copy(
      core.copy(parentIds = core.parentIds ++ ids),
    )

    /** Add related spec IDs. Supports multiple IDs at once via varargs. */
    def related(ids: String*): Stage2 = copy(
      core.copy(relatedToIds = core.relatedToIds ++ ids),
    )

    /** Set the status of the spec (e.g., draft, verified, etc). */
    def status(s: String): Stage2 = copy(core.copy(status = Some(s)))

    /** Set the implementer of the spec (e.g., module/class name). */
    def impl(by: String): Stage2 = copy(core.copy(implementedBy = Some(by)))

    /** Set the verifier of the spec (e.g., test/verification reference). */
    def verified(by: String): Stage2 = copy(core.copy(verifiedBy = Some(by)))

    /**
     * Add required Capability IDs for the spec. Supports multiple IDs at once
     * via varargs.
     */
    def requiresCaps(ids: String*): Stage2 = copy(
      core.copy(requiredCaps = core.requiredCaps ++ ids),
    )

    /**
     * Add additional metadata key-value pairs. Supports multiple pairs at once
     * via varargs.
     */
    def meta(kv: (String, String)*): Stage2 = copy(
      core.copy(metadata = core.metadata ++ kv.toMap),
    )

    /** Add a spec entry (name-value pair) to the spec. */
    def entry(name: String, value: String): Stage2 =
      copy(core.copy(entries = core.entries :+ SpecEntry(name, value)))

    // --- TERMINAL: Complete the builder and emit the spec ---
    /**
     * Complete the builder and create the final [[HardwareSpecification]]
     * object. Performs runtime validation, emits the .spec file, and registers
     * the spec.
     * @return
     *   Fully defined HardwareSpecification object
     */
    def build(scalaDeclarationPath: Option[String] = None): HardwareSpecification = {
      // 1. Validation (runtime validation beyond compile-time safety)
      //    - Ensure the spec ID is URI-safe (no spaces)
      //    - Ensure at least one entry is defined
      require(
        !core.id.contains(" "),
        s"Spec ID '${core.id}' must not contain spaces. Use URI-safe characters.",
      )
      require(
        core.entries.nonEmpty,
        s"Spec '${core.id}' must define at least one entry.",
      )
      //    - (Optional) Check for duplicate IDs via SpecRegistry (not yet implemented)
      //      This can be extended in SpecRegistry or handled at the plugin aggregation stage.

      // 2. Create the final HardwareSpecification object from the immutable Core object
      val spec = core.toHardwareSpec.copy(scalaDeclarationPath = scalaDeclarationPath)

      // 3. Side effects:
      //    - Emit the .spec file for plugin aggregation (compile-time metadata)
      //    - Register the spec in SpecRegistry (for debugging and pre-plugin reference)
      MetaFile.writeSpec(spec)
      SpecRegistry.addSpec(spec)

      spec // Return the final HardwareSpecification object
    }

    // Internal helper: copy the current Stage2's Core object to create a new Stage2 instance (immutability).
    private def copy(c: Core) = new Stage2(c)
  }

  // --------------------------------------------------------------------------
  // Internal immutable aggregator: Core
  // Aggregates all fields of the spec in an immutable state during builder stages.
  // Each builder method creates a new copy of Core with updated fields.
  // --------------------------------------------------------------------------
  private final case class Core(
    id: String,
    cat: SpecCategory,
    desc: String,
    capability: Option[Capability] = None,
    status: Option[String] = None,
    metadata: Map[String, String] = Map.empty,
    parentIds: Set[String] = Set.empty,
    relatedToIds: Set[String] = Set.empty, // Related spec IDs
    implementedBy: Option[String] = None,
    verifiedBy: Option[String] = None,
    requiredCaps: Set[String] = Set.empty, // Required capabilities
    definitionFile: Option[String] = None,
    entries: List[SpecEntry] = Nil,
  ) {
    // Convert the Core object to the final HardwareSpecification object
    def toHardwareSpec: HardwareSpecification =
      HardwareSpecification(
        id = id,
        category = cat,
        description = desc,
        capability = capability,
        status = status,
        metadata = metadata,
        parentIds = parentIds,
        relatedToIds = relatedToIds,
        implementedBy = implementedBy,
        verifiedBy = verifiedBy,
        requiredCapabilities = requiredCaps,
        definitionFile = definitionFile,
        entries = entries,
        scalaDeclarationPath = None // default, will be set in build() if needed
      )
  }
}
