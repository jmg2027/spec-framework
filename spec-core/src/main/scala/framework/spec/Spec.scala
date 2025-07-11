// spec-core/src/main/scala/framework/spec/Spec.scala
// Simplified staged DSL for building HardwareSpecification objects.
package framework.spec

object Spec {
  // Entry points per category
  def CONTRACT(id: String): Stage1  = new Stage1(SpecCategory.CONTRACT, id)
  def FUNCTION(id: String): Stage1  = new Stage1(SpecCategory.FUNCTION, id)
  def PROPERTY(id: String): Stage1  = new Stage1(SpecCategory.PROPERTY, id)
  def COVERAGE(id: String): Stage1  = new Stage1(SpecCategory.COVERAGE, id)
  def INTERFACE(id: String): Stage1 = new Stage1(SpecCategory.INTERFACE, id)
  def PARAMETER(id: String): Stage1 = new Stage1(SpecCategory.PARAMETER, id)
  def CAPABILITY(id: String): Stage1= new Stage1(SpecCategory.CAPABILITY, id)
  def RAW(id: String, prefix: String): Stage1 = new Stage1(SpecCategory.RAW(prefix), id)

  // stage1: require description
  final class Stage1 private[Spec](cat: SpecCategory, id: String) {
    def desc(d: String): Stage2 = new Stage2(Core(id, cat, d))
  }

  // stage2: optional fields and build
  final class Stage2 private[Spec](core: Core) {
    private def idsFrom(args: Seq[Any], enforceContract: Boolean = false): Set[String] = {
      args.map {
        case s: HardwareSpecification =>
          if (enforceContract && (core.cat != SpecCategory.CONTRACT || s.category != SpecCategory.CONTRACT))
            throw new IllegalArgumentException("uses is allowed only between CONTRACT specs")
          s.id
        case s: String =>
          if (enforceContract && core.cat != SpecCategory.CONTRACT)
            throw new IllegalArgumentException("uses is allowed only between CONTRACT specs")
          s
        case other =>
          throw new IllegalArgumentException(s"Invalid reference: $other")
      }.toSet
    }

    def is(refs: Any*): Stage2 = copy(core.copy(is = core.is ++ idsFrom(refs)))
    def has(refs: Any*): Stage2 = copy(core.copy(has = core.has ++ idsFrom(refs)))
    def uses(refs: Any*): Stage2 = copy(core.copy(uses = core.uses ++ idsFrom(refs, enforceContract = true)))
    def status(s: String): Stage2 = copy(core.copy(status = Some(s)))
    def entry(name: String, value: String): Stage2 = copy(core.copy(lists = core.lists :+ (name -> value)))
    def table(t: String): Stage2 = copy(core.copy(tables = core.tables :+ t))
    def draw(d: String): Stage2 = copy(core.copy(drawings = core.drawings :+ d))
    def code(c: String): Stage2 = copy(core.copy(codes = core.codes :+ c))
    def note(n: String): Stage2 = copy(core.copy(notes = core.notes :+ n))

    def build(scalaDeclarationPath: String = ""): HardwareSpecification = {
      require(!core.id.contains(" "), s"Spec ID '${core.id}' must not contain spaces")
      require(core.desc.nonEmpty, "description must be provided via desc")
      val spec = core.toHardwareSpec.copy(scalaDeclarationPath = scalaDeclarationPath)
      MetaFile.writeSpec(spec)
      SpecRegistry.addSpec(spec)
      spec
    }

    private def copy(c: Core) = new Stage2(c)
  }

  // internal core aggregator
  private final case class Core(
      id: String,
      cat: SpecCategory,
      desc: String,
      status: Option[String] = None,
      is: Set[String] = Set.empty,
      has: Set[String] = Set.empty,
      uses: Set[String] = Set.empty,
      lists: List[(String, String)] = Nil,
      tables: List[String] = Nil,
      drawings: List[String] = Nil,
      codes: List[String] = Nil,
      notes: List[String] = Nil,
  ) {
    def toHardwareSpec: HardwareSpecification =
      HardwareSpecification(
        id = id,
        category = cat,
        description = desc,
        status = status,
        is = is,
        has = has,
        uses = uses,
        lists = lists,
        tables = tables,
        drawings = drawings,
        codes = codes,
        notes = notes,
        scalaDeclarationPath = ""
      )
  }
}
