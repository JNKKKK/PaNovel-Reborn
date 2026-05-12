#!/bin/sh
# Usage: ./bump-version.sh 1.2.3
# Increments version_code and sets version_name in version.properties, then commits.
set -e
cd "$(dirname "$0")"

versionName=$1
if [ -z "$versionName" ]; then
    echo "Usage: $0 <version>" >&2
    exit 1
fi

versionFile="version.properties"

versionCode=$(sed -n 's/\s*version_code\s*=\s*\(\S*\)/\1/p' "$versionFile")
versionCode=$(expr "$versionCode" + 1)

sed -i "s/version_code\s*=\s*[0-9]*/version_code=$versionCode/" "$versionFile"
sed -i "s/version_name\s*=\s*.*/version_name=$versionName/" "$versionFile"

git add "$versionFile"
git commit -m "Bump version to $versionName"
