# AGENTS instructions
All code changes must be validated by running `./publish.sh` at the repository root.
Do not run other sbt commands directly during testing.
After ./publish.sh completes, confirm that `design/target/SpecIndex.json` and
`design/target/TagIndex.json` exist and contain valid JSON (e.g. using
`jq -e`).
Additionally verify that both JSON files contain at least one entry:

```bash
jq 'length > 0' design/target/SpecIndex.json
jq 'length > 0' design/target/TagIndex.json

# Confirm that each file contains the expected object structure
jq -e '.[0] | has("id") and has("category")' design/target/SpecIndex.json
jq -e '.[0] | has("scalaDeclarationPath") and has("srcFile")' design/target/TagIndex.json

# Compare against golden files (sorted for deterministic ordering)
jq -S 'sort_by(.id)' design/target/SpecIndex.json > design/target/SpecIndex.sorted.json
jq -S 'sort_by(.id)' design/target/TagIndex.json  > design/target/TagIndex.sorted.json
diff -u design/golden/SpecIndex.golden.json design/target/SpecIndex.sorted.json
diff -u design/golden/TagIndex.golden.json  design/target/TagIndex.sorted.json
```
