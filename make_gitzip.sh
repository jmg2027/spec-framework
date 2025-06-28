#!/bin/bash

# usage: ./make_gittar.sh [zipname]
# default: git_repo_YYYYMMDD.zip

set -e
# Set default extension to .zip
ZIPNAME="${1:-git_repo_$(date +%Y%m%d).zip}"

# git tracked files zipped
git ls-files | zip -@ "$ZIPNAME"

echo "Created: $ZIPNAME"

if command -v open >/dev/null 2>&1; then
    open "$ZIPNAME"
elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$ZIPNAME"
else
    echo "Cannot automatically open $ZIPNAME. Please open it manually."
fi