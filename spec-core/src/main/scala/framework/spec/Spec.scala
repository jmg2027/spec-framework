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
  def BUNDLE(id: String): Stage1    = new Stage1(SpecCategory.BUNDLE, id)
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
    
    // Unified table method with type specification
    def table(tableType: String, content: String): Stage2 = {
      val markdown = tableType.toLowerCase match {
        case "markdown" | "md" => content
        case "csv" => 
          val lines = content.split("\n")
          if (lines.length >= 2) {
            val header = "| " + lines(0).split(",").mkString(" | ") + " |"
            val separator = "|" + lines(0).split(",").map(_ => "---").mkString("|") + "|"
            val rows = lines.drop(1).map(line => "| " + line.split(",").mkString(" | ") + " |").mkString("\n")
            List(header, separator, rows).mkString("\n")
          } else content
        case _ => content // fallback to raw content
      }
      copy(core.copy(tables = core.tables :+ markdown))
    }
    
    // Convenience method for structured tables
    def markdownTable(headers: List[String], rows: List[List[String]]): Stage2 = {
      val headerRow = "| " + headers.mkString(" | ") + " |"
      val separator = "|" + headers.map(_ => "---").mkString("|") + "|"
      val dataRows = rows.map(row => "| " + row.mkString(" | ") + " |").mkString("\n")
      val markdown = List(headerRow, separator, dataRows).mkString("\n")
      copy(core.copy(tables = core.tables :+ markdown))
    }
    
    // Unified drawing method with type specification
    def draw(drawType: String, content: String): Stage2 = {
      val markdown = drawType.toLowerCase match {
        case "mermaid" => s"```mermaid\n$content\n```"
        case "svg" => s"```svg\n$content\n```"
        case "ascii" | "text" => content
        case "plantuml" => s"```plantuml\n$content\n```"
        case _ => content // fallback to raw content
      }
      copy(core.copy(drawings = core.drawings :+ markdown))
    }
    
    def code(language: String, content: String): Stage2 = {
      val markdown = s"```$language\n$content\n```"
      copy(core.copy(codes = core.codes :+ markdown))
    }
    def code(content: String): Stage2 = code("text", content) // default to text
    
    def note(n: String): Stage2 = copy(core.copy(notes = core.notes :+ n))

    // Entry method supporting both key-value pairs and hierarchical lists
    def entry(key: String, value: String = ""): Stage2 = {
      if (value.isEmpty) {
        // Treat as simple list item (hierarchical level can be indicated with leading spaces in key)
        copy(core.copy(lists = core.lists :+ (key -> "")))
      } else {
        // Key-value pair
        copy(core.copy(lists = core.lists :+ (key -> value)))
      }
    }

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
