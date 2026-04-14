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
- **PR builds**: `.github/workflows/pr-build.yml` — runs `mvn clean verify` on ubuntu-latest with JDK 17
- **Releases**: `.github/workflows/release.yml` — triggered by `v*` tags from main branch, builds and creates GitHub Release with the assembly zip

## Git
```bash
git status
git log --oneline -20
git diff
```
