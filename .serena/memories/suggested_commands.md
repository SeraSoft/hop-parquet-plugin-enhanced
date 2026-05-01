# Suggested Commands

## Build
```bash
# Full build (requires GitHub Packages auth via GITHUB_ACTOR/GITHUB_TOKEN env vars)
mvn clean package -s .m2/settings.xml

# Verify (used by CI on PRs)
mvn clean verify -s .m2/settings.xml

# Compile only
mvn clean compile -s .m2/settings.xml
```

**Important**: Always use `-s .m2/settings.xml` — the project uses a custom Maven settings file that references GitHub Packages for Hop artifacts from `maven.pkg.github.com/serasoft/hop`.

## No Tests
There are currently **no unit tests** in the project (`src/test/` does not exist).

## CI/CD
- **CI** (`.github/workflows/ci.yml`): runs `mvn clean verify` on ubuntu-latest with JDK 17 on PRs and pushes to `main`. On `main` it uploads a `snapshot-<sha>` artifact (5-day retention).
- **Releases** (`.github/workflows/release.yml`): triggered by SemVer tags `v[0-9]+.[0-9]+.[0-9]+*`. Builds with `-Drevision=${VERSION}`, extracts the matching `CHANGELOG.md` section, computes sha256, publishes a GitHub Release. Pre-release flag is set automatically when the tag has a `-` suffix. See `docs/guides/tech/release-process.md`.
- GitHub Actions are pinned to Node.js 24-compatible major versions (`actions/checkout@v5`, `actions/setup-java@v5`, `actions/upload-artifact@v4`, `softprops/action-gh-release@v2`).

## Git
```bash
git status
git log --oneline -20
git diff
```
