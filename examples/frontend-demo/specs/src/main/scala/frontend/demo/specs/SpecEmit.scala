// examples/frontend-demo/specs/.../frontend/demo/specs/SpecEmit.scala
// -----------------------------------------------------------------------------
//  Forces runtime emission of every spec in the graph into spec.meta.dir.
//  Touching any member of `FetchSpecs` runs its object initializer, which
//  evaluates every `val` (including the typed bundles) and therefore writes each
//  `.spec`. Non-typed specs are also emitted at compile time by `spec { … }`;
//  re-emitting here is harmless (the checker de-dupes by id).
// -----------------------------------------------------------------------------
package frontend.demo.specs

object SpecEmit {
  def main(args: Array[String]): Unit = {
    // Reference the whole graph so the object initializer runs end to end.
    val n =
      List(
        FetchSpecs.paramPcWidth, FetchSpecs.paramTxnId,
        FetchSpecs.bndFetchReq, FetchSpecs.bndFetchResp, FetchSpecs.bndInstrSlot,
        FetchSpecs.intfEpmReqOut, FetchSpecs.intfEpmRespIn,
        FetchSpecs.funcFetchRequest, FetchSpecs.funcFetchResponse,
        FetchSpecs.contFetchUnit,
        FetchSpecs.propNoOverflow, FetchSpecs.propInOrderResp,
        FetchSpecs.propEpochClean, FetchSpecs.covBackpressure,
      ).map(_.id).distinct.size
    println(s"[SpecEmit] emitted $n spec nodes to ${sys.props.getOrElse("spec.meta.dir", "<unset>")}")
  }
}
