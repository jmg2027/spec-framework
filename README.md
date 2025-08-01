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

## 2025 Updates

### Enhanced Features

The Spec Framework has been significantly improved with the following enhancements:

#### 🔄 Relaxed uses() Method Constraints

- **PARAMETER specs** can now be referenced by any category (BUNDLE, FUNCTION, INTERFACE, etc.)
- **CONTRACT specs** can reference other CONTRACT specs for hierarchical system modeling
- Better error messages for invalid relationships

#### 📊 Improved table() Method

- **Single parameter version**: `.table(content)` defaults to markdown
- **Two parameter version**: `.table(type, content)` for explicit format specification
- Full backward compatibility maintained

#### 🧪 Comprehensive Testing

- All new features thoroughly tested
- Validation of complex spec relationships
- Backward compatibility verified

For detailed information on the new features, see [`docs/enhanced_builder_guide.md`](docs/enhanced_builder_guide.md).

## License

This project is licensed under the MIT License.
Until early 2025 the repository used a proprietary license for asset management between the company and the repository owner. We transitioned to MIT later that year to encourage broader community participation.
See [LICENSE](LICENSE) for details.
