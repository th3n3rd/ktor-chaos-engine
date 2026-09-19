# https://just.systems

# Bump the project to the next major, minor or patch version
bump version:
    #!/usr/bin/env bash
    set -euo pipefail

    next="$(svu {{ version }})"
    git tag -s -a "$next" -m "Release $next"

    echo "Created tag $next"

# Release the current project version
release:
    #!/usr/bin/env bash
    git push origin $(svu current)

# Show the current project version
current-version:
    #!/usr/bin/env bash
    set -euo pipefail
    svu current

# Run all the tests
test:
    #!/usr/bin/env bash
    set -euo pipefail
    ./gradlew test