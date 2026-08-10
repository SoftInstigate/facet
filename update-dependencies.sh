#!/usr/bin/env bash

set -euo pipefail

# If the first argument is true, allow minor updates.
ALLOW_MINOR_UPDATES=${1:-false}
# Ignore prerelease qualifiers by default (M, RC, alpha, beta, EA).
# Override with MAVEN_VERSION_IGNORE if needed.
IGNORED_VERSIONS=${MAVEN_VERSION_IGNORE:-".+-M[0-9]*,.*-RC[0-9]*,.*-alpha[0-9]*,.*-beta[0-9]*,.*-ea[0-9]*"}
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

if [[ -x "${SCRIPT_DIR}/mvnw" ]]; then
	MVN_CMD=("${SCRIPT_DIR}/mvnw")
else
	MVN_CMD=(mvn)
fi

COMMON_ARGS=(
	-DallowMinorUpdates="${ALLOW_MINOR_UPDATES}"
	-DincludePlugins=true
	-Dmaven.version.ignore="${IGNORED_VERSIONS}"
)

echo "Updating dependencies with allowMinorUpdates=${ALLOW_MINOR_UPDATES}"
echo "Ignoring prerelease versions matching: ${IGNORED_VERSIONS}"

cd "${SCRIPT_DIR}"

# Versions Plugin goals that modify pom.xml are more reliable when run separately.
"${MVN_CMD[@]}" versions:use-latest-releases "${COMMON_ARGS[@]}"
"${MVN_CMD[@]}" versions:update-properties "${COMMON_ARGS[@]}"