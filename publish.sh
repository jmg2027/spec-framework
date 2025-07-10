#!/bin/bash

# publish.sh
# This script automates the publishing of all components of the hardware specification management framework
# to the local Maven/Ivy repository.
# It performs a clean initialization of the build environment and builds projects in the correct dependency order.

# 1. Delete all SBT-related caches and build artifacts (strong cleanup)
echo "--- 1/5: Deleting all SBT caches and build artifacts ---"
rm -rf target/ project/target/
rm -rf spec-core/target/ spec-macros/target/ spec-plugin/target/ design/target/
rm -rf .bloop/
rm -rf ~/.ivy2/local/your.company/ # Force delete locally published 'your.company' artifacts
rm -rf ~/.sbt/boot/ ~/.sbt/cache/ ~/.sbt/plugin/ # Delete SBT boot and global caches
echo "Caches and artifacts deleted."
echo ""

# 2. Publish spec-core locally (for all cross Scala versions)
# spec-macros and spec-plugin depend on spec-core, so it must be published first.
echo "--- 2/5: Publishing spec-core locally (all Scala versions) ---"
sbt "+specCore / publishLocal"
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to publish spec-core. Aborting script."
    exit 1
fi
echo "spec-core published successfully."
echo ""

# 3. Publish spec-macros locally (depends on spec-core)
# The design project depends on spec-macros, so it must be published after spec-core.
echo "--- 3/5: Publishing spec-macros locally (main Scala version) ---"
sbt "+specMacros / publishLocal"
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to publish spec-macros. Aborting script."
    exit 1
fi
echo "spec-macros published successfully."
echo ""


# 4. Publish spec-plugin locally (depends on spec-core)
# The design project loads spec-plugin as a plugin, so it must be published last.
echo "--- 4/5: Publishing spec-plugin locally ---"
# Execute publishLocal within an SBT shell for the specPlugin project
sbt "project specPlugin" "publishLocal"
# sbt <<EOF
# project specPlugin
# publishLocal
# exit
# EOF
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to publish spec-plugin. Aborting script."
    exit 1
fi
echo "spec-plugin published successfully."
echo ""

# 5. Execute the spec index generation task directly in the design project
echo "--- 5/5: Executing exportSpecIndex task in design project ---"
cd design
# (MODIFIED): Directly run the exportSpecIndex task of the design project.
# This task internally triggers compilation, reads .tag and .spec files, and generates JSON.
sbt "exportSpecIndex"
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to execute exportSpecIndex task in design project. Aborting script."
    exit 1
fi
cd .. # Return to root directory

echo ""
echo "--- All build and publish processes completed ---"
echo "Check SpecIndex.json and TagIndex.json files in the design/target/ directory."
