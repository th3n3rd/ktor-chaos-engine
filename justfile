# https://just.systems

# Bump the project to the next major, minor or patch version
bump version:
    #!/usr/bin/env bash
    set -euo pipefail

    next="$(svu {{ version }})"
    git tag -a "$next" -m "Release $next"

    echo "Created tag $next"

# Show the current project version
current-version:
    #!/usr/bin/env bash
    set -euo pipefail
    svu current

