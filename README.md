# Spec Framework

This repository contains a proof-of-concept hardware specification management framework.
It is organized as several sbt subprojects:

- **spec-core** – definition of specification data types
- **spec-macros** – macro annotations that collect spec information
- **spec-plugin** – sbt plugin for exporting JSON indexes
- **design** – example project using the framework

The DSL offers categories such as `CONTRACT`, `FUNCTION`, and `INTERFACE`.  The
`BUNDLE` category can be used to document reusable data structures referenced by
interfaces.

To build everything offline run:

```bash
./publish.sh
```

After running the script, the files `SpecIndex.json` and `TagIndex.json` will be created in the output directory specified by the `spec.meta.dir` system property in your `build.sbt`. By default, this is `design/target/`, but it may be customized by the user.

See `docs/architecture.md` for an overview, `docs/PLUGIN_USAGE.md` for sbt setup,
`docs/user_guide_213.md` for usage instructions and `docs/developer_guide.md` for
details on hacking the framework. Coding style conventions are documented in
`docs/SCALADOC_STYLE_GUIDE.md`.

For English documentation, refer to
[`docs/plugin_enable_en.md`](docs/plugin_enable_en.md),
[`docs/builder_usage_en.md`](docs/builder_usage_en.md) and
[`docs/localspec_usage_en.md`](docs/localspec_usage_en.md).

For Korean documentation, refer to
[`docs/plugin_enable_ko.md`](docs/plugin_enable_ko.md),
[`docs/builder_usage_ko.md`](docs/builder_usage_ko.md) and
[`docs/localspec_usage_ko.md`](docs/localspec_usage_ko.md).

## License

This project is proprietary and intended for the exclusive personal use of the repository owner.
Forking, distribution, and external contributions are prohibited without explicit written permission.
