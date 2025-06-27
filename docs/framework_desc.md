# ChiselSpecVerify Framework Specification (rev E)

> **Mission statement**  Unify architectural specifications, Chisel RTL, and verification artefacts inside a single Scala‑based source of truth, with maximum automation and minimum manual wiring.

---

## 0 Terminology

| Term                 | Meaning                                                                                                                                   |
| -------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| **Spec Object**      | A Scala `object` or `val` extending `HardwareSpecification`; it holds *what* must be true (requirements, properties, coverage goals).     |
| **@LocalSpec tag**   | An annotation attached to *RTL / test code* that indicates *where* and *how* a SpecObject is implemented / exercised.                     |
| **TagCat**           | Closed enumeration of top‑level spec categories: `FUNC`, `PROP`, `COV`, `PARAM`, `CONTRACT` …                                             |
| **Capability**       | Abstract functional role label (e.g. `BASIC_QUEUE`, `RVFI_TRACE`). Upper layer may depend on a role instead of a concrete implementation. |
| **CONTRACT spec**    | Optional **representative** spec describing a module‑level contract; category =`CONTRACT`, `isRepresentative =true`.                      |
| **SpecIndex.json**   | Build‑generated flat catalogue – one JSON object per Spec tag, enriched with instance paths, parent links, coverage status …              |
| **ModuleIndex.json** | Derived view grouping SpecIndex by `ownerModule`; useful for *module‑centric* browsing.                                                   |

---

## 1 Scala API (primitives)

### 1.1 `TagCat` enumeration

```scala
sealed trait TagCat { def prefix: String }
object TagCat {
  case object FUNC      extends TagCat { val prefix = "FUNC" }
  case object PROP      extends TagCat { val prefix = "PROP" }
  case object COV       extends TagCat { val prefix = "COV"  }
  case object PARAM     extends TagCat { val prefix = "PARAM"}
  case object CONTRACT  extends TagCat { val prefix = "CONTRACT" } // representative
  final case class Raw(prefix: String) extends TagCat               // ad‑hoc
}
```

### 1.2 `Capability` (type‑safe label)

```scala
sealed trait Capability { def name: String }
object Capability {
  case object BASIC_QUEUE  extends Capability { val name = "BASIC_QUEUE"  }
  case object LOWPWR_QUEUE extends Capability { val name = "LOWPWR_QUEUE" }
  case object EXT_IP       extends Capability { val name = "EXT_IP"       }
  final case class Raw(name: String) extends Capability
}
```

### 1.3 `HardwareSpecification` trait

```scala
trait HardwareSpecification {
  // --- mandatory ---
  def description: String                     // human‑readable
  // --- optional metadata ---
  def capability         : Capability  = null // functional role label (mainly for CONTRACT)
  def parentSpecIds      : Set[String] = Set.empty
  def requiredCaps       : Set[String] = Set.empty // upper‑layer demand
  def requiredParamMap   : Map[String,String] = Map.empty // depth = 32 …
  def metadata           : Map[String,String] = Map.empty // open bag for team specifics
}
```
scala-reflect macroTransform white-box annotation.

---

## 2 `@LocalSpec` annotation (implementation tag)

```scala
class LocalSpec(
  cat        : TagCat,
  localId    : String,                    // e.g. "FULL_FLAG-001" or "BASIC_QUEUE"
  capability : Capability = null,         // for CONTRACT only (else ignored)
  submods    : Seq[Class[_]] = Nil        // *optional* override: declare that this RTL block instantiates
                                          // these module classes even if the instance graph cannot see them.
) extends scala.annotation.StaticAnnotation
```
Implementation: scala.reflect.macros.whitebox.Context 기반 매크로. Requires paradise + -Ymacro-annotations. Scalameta 의존 없음.

**Guidelines**

* Attach `@LocalSpec` **only** to RTL / test *definitions* (`val`, `def`, `lazy val`, anonymous code block) – *not* to the SpecObject itself.
* `submods` is a *last‑resort escape hatch* : *Use only* if the instance graph is obscure (generated dynamic module) and the automatic path scanner would miss the inclusion. *Omit* in >95 % of cases.

---

## 3 Spec definition vs. implementation — canonical examples

### 3.1 Spec Object (definition only)

```scala
// QueueSpecs.scala  – SPEC DEFINITIONS (no @LocalSpec here!)
object QueueSpecs {
  /** Representative contract */
  val Contract = new HardwareSpecification {
    val description = "Provides FIFO buffering with 1‑cycle latency and full/empty signals."
    override val capability = Capability.BASIC_QUEUE
  }

  /** Property: full flag correctness */
  val PropFullFlag = new HardwareSpecification {
    val description = "When depth slots are occupied, full must assert on next cycle."
    override val parentSpecIds = Set("klase32.util.spec.QueueSpecs-CONTRACT-BASIC_QUEUE")
  }
}
```

### 3.2 RTL implementation with `@LocalSpec`

```scala
// Queue.scala – RTL implementation (extract)
class Queue(depth: Int = 4, w: Int = 32) extends Module {
  val io = IO(new Bundle { ... })

  /* CONTRACT implementation tag */
  this.suggestName("Queue_impl")
  @LocalSpec(TagCat.CONTRACT, "BASIC_QUEUE", capability = Capability.BASIC_QUEUE)
  val contractTag = () // dummy val – location of the module‑level contract

  /* Property implementation tag inside logic */
  @LocalSpec(TagCat.PROP, "FULL_FLAG-001")
  assert(!(full && io.enq.ready))
}
```

*SpecObjects are imported only for ****parent linkage / test references****, never annotated.*

---

## 4 Build‑time pipeline (overview)

| Step                           | Action                                                                                                                                                                      |
| ------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **① Compilation**              | Scala compiler expands all `@LocalSpec` (scala-reflect macro-annotation); macro captures file/line, ownerModule, instance path placeholder.                                                                  |
| **② Elaboration → FIRRTL**     | Pass‑through annotations emit exact `instancePath`.                                                                                                                         |
| **③ exportSpecIndex** SBT task | Combines macro metadata + FIRRTL instance graph.• auto‑link *Spec.parentIds* (instance‑contain + CONTRACT hierarchy).• writes `target/SpecIndex.json` & `ModuleIndex.json`. |
| **④ specLint (optional)**      | Warn / fail for: missing CONTRACT per module, unused Spec, etc. Controlled via JVM flags.                                                                                   |
| **⑤ Verification run**         | chiseltest / Jasper / sim harness emit `verifications.csv` (see §5).                                                                                                        |
| **⑥ reportGen**                | Merges JSON + verification logs into HTML dashboard & coverage matrix.                                                                                                      |

---

## 5 Verification & coverage mapping

`verifications` field consolidates evidence that a spec is *exercised & proved*.

```json
{
  "canonicalId" : "…QueueSpecs-PROP-FULL_FLAG-001",
  "verifications": [
    {
      "tool"   : "chiseltest",
      "suite"  : "QueueTester",
      "test"   : "fullFlagWrap",
      "status" : "PASS"
    },
    {
      "tool"   : "jaspergold",
      "prop"   : "Queue_fullFlag",
      "k"      : 20,
      "status" : "PASS",
      "time_ms": 1823
    }
  ]
}
```

*Structure is ****open**** – each CI adapter can append its own JSON objects.*

---

## 6 SpecIndex.json schema (excerpt)

```json
{
  "canonicalId"       : string,   // unique FQN‑ID
  "prettyId"          : string,
  "category"          : "FUNC"|"PROP"|…|"CONTRACT",
  "isRepresentative"  : boolean,  // only for CONTRACT
  "capability"        : string?,
  "ownerModule"       : string,   // fqcn of defining scala object
  "definitionFile"    : string?,  // for debugging only
  "instancePaths"     : [string], // where this tag appears in RTL
  "parentIds"         : [string],
  "containedModuleIds": [string], // direct children (A‑model)
  "verifications"     : [object]  // see §5
}
```

---

## 7 ModuleIndex.json (derived)

```json
{
  "klase32.util.design.Queue": {
    "instancePaths": ["…/CPU/FetchQueue/Queue1", "…/LSU/Queue2"],
    "specs"        : ["…CONTRACT-BASIC_QUEUE", "…PROP-FULL_FLAG-001", …]
  }
}
```

---

## 8 Lint rule examples

```bash
# Warn only (default)
$ sbt specLint

# Fail build if any module lacks CONTRACT spec
$ sbt -Dspec.fail.noContract=true specLint
```

---

## 9 Version history

| Rev   | Date       | Notes                                                                                                                        |
| ----- | ---------- | ---------------------------------------------------------------------------------------------------------------------------- |
| **E** | 2025‑07‑01 | *Migrate LocalSpec to scala-reflect macros; drop Scalameta.* |
| **D** | 2025‑06‑26 | *Separated spec definition vs. implementation; CONTRACT category; Capability enum; verifications schema; submods clarified.* |
| C     | 2025‑06‑25 | Initial public draft with CONTRACT & Capability concepts                                                                     |
| B     | 2025‑06‑24 | children param dropped; A‑model clarified                                                                                    |
| A     | 2025‑06‑23 | First integrated description                                                                                                 |

---

### Open extension points (road‑map)

| Idea                                                                                                                                                  | Motivation                                                                                                                                                                                                         | Implementation sketch                                                                                                                                                                                           |
| ----------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| \*\*Param‑aware \*\*\`\`  add optional `paramValues: Map[String,String]` parameter → eliminates need for dummy `val` and `if(depth==...)` constructs. | Contract spec remains one object; macro injects constructor‑param values at annotation site and synthesises canonicalId suffix (e.g. `BASIC_QUEUE@DEPTH32`). Simplifies RTL, preserves per‑parameter traceability. | 1. Macro reflects `this` (Module) params.  2. Fills `paramValues` map when omitted.  3. CanonicalId builder appends `key=value` pairs in stable order.  4. `SpecIndex` JSON gains `params` field for filtering. |
| **Security / DFT categories**                                                                                                                         | Extend `SpecCategory` with `SEC`, `DFT`.                                                                                                                                                                           | Pure enum growth.                                                                                                                                                                                               |
| **Scala 3 macro migration**                                                                                                                           | Eliminate paradise plugin; enable true stat‑level annotation (attach to `when` / `elsewhen` directly).                                                                                                             | Requires tool‑chain switch; backlog item after RTL freeze.                                                                                                                                                      |
