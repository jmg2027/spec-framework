package spec.plugin
import sbt._, Keys._
import spec.core.SpecRegistry
import upickle.default.{write => uwrite}

object SpecPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  object autoImport {
    val exportSpecIndex = taskKey[Unit]("emit SpecIndex.json")
  }
  import autoImport._
  override lazy val projectSettings = Seq(
    exportSpecIndex := {
      val out = (Compile/target).value / "SpecIndex.json"
      IO.write(out, uwrite(SpecRegistry.all, indent = 2))
      streams.value.log.info(s"spec-index -> $out")
    },

    (Test / test) := (Test / test).dependsOn(exportSpecIndex).value
  )
}
