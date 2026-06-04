package streamproc

import chisel3._
import framework.spec.SpecCheck.BundleWidths
import streamproc.design._

/** Captures the concrete field widths of each typed bundle (chisel3 .getWidth)
  * and writes BundleWidths.json, which SpecCheck merges so the spec shows
  * `data: UInt(32)` instead of the macro's static `data: UInt` (closes the gap
  * that the type alone has no config-derived width). */
object WidthProbe {
  private def widths(b: => Bundle): List[(String, String)] =
    b.elements.toList.reverse.map { case (n, d) => (n, s"${d.getClass.getSimpleName}(${d.getWidth})") }

  def main(args: Array[String]): Unit = {
    val c = SPConfig()
    val all = List(
      BundleWidths("BND_STREAM_BEAT", widths(new StreamBeat(c))),
      BundleWidths("BND_PACKET_META", widths(new PacketMeta(c))),
      BundleWidths("BND_TAGGED_BEAT", widths(new TaggedBeat(c))),
      BundleWidths("BND_RULE_ACTION", widths(new RuleAction(c))),
    )
    val dir = java.nio.file.Paths.get(sys.props.getOrElse("spec.meta.dir", "."))
    java.nio.file.Files.createDirectories(dir)
    java.nio.file.Files.write(dir.resolve("BundleWidths.json"), upickle.default.write(all, indent = 2).getBytes)
    println(s"[width-probe] wrote BundleWidths.json for ${all.size} bundles")
  }
}
