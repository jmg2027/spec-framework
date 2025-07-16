// build.sbt in RTL project
resolvers += "jitpack" at "https://jitpack.io"      // if using JitPack
addSbtPlugin("com.github.<org>" % "spec-plugin" % "v0.1.0")   // AutoPlugin
libraryDependencies += "com.github.<org>" %% "spec-core" % "v0.1.0"

import annotations.LocalSpec
@LocalSpec(SpecCategory.CONTRACT,"BASIC_QUEUE", capability=Capability.BASIC_QUEUE)
class Queue32 extends Module { … }

sbt test → target/SpecIndex.json


## License

This project is proprietary and intended for the exclusive personal use of the repository owner. Forking, distribution, and external contributions are prohibited without explicit written permission.
