# Spec Framework

This repository contains a proof-of-concept hardware specification management framework.
It is organized as several sbt subprojects:

- **spec-core** – definition of specification data types
- **spec-macros** – macro annotations that collect spec information
- **spec-plugin** – sbt plugin for exporting JSON indexes
- **design** – example project using the framework

To build everything offline run:

```bash
./publish.sh
```

After running the script the files `design/target/SpecIndex.json` and
`design/target/TagIndex.json` will be created.

See `docs/architecture.md` for an overview, `docs/PLUGIN_USAGE.md` for sbt setup,
`docs/user_guide_213.md` for usage instructions and `docs/developer_guide.md` for
details on hacking the framework. Coding style conventions are documented in
`docs/SCALADOC_STYLE_GUIDE.md`.

## License

This project is proprietary and intended for the exclusive personal use of the repository owner.
Forking, distribution, and external contributions are prohibited without explicit written permission.
