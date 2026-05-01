# Release Management Alignment Design

**Date**: 2026-05-01
**Topic**: Align release management of `hop-parquet-plugin-enhanced` with the model implemented in `hop-process-template-plugins`.

## Goal

Bring this project's release engineering in line with `hop-process-template-plugins`:

- Maven *CI Friendly Versions* (`${revision}`) so the version is injected at build time from the git tag.
- Two GitHub workflows: a `CI` workflow (PR + push to `main`, with snapshot artifact upload) and a `Release` workflow driven by SemVer-validated tags, with automatic changelog extraction, SHA-256 computation, and pre-release detection.
- A `CHANGELOG.md` in Keep-a-Changelog 1.1.0 format with a staged `[Unreleased]` section.
- A `docs/guides/tech/release-process.md` operator-facing guide.
- Updated `CLAUDE.md` with the same expanded Serena rules used in the template, plus a changelog-update entry in the Task Completion Checklist.

## Decisions taken during brainstorming

1. **Versioning scheme**: SemVer **independent** of Apache Hop. `<revision>` starts at `1.0.0-SNAPSHOT`. Hop's version stays only inside dependency declarations.
2. **CI workflow**: full adoption of the template's `ci.yml` (PR + push to `main`, snapshot artifact, no failure-comment step). The current `pr-build.yml` is removed.
3. **Initial CHANGELOG**: includes both an empty `[Unreleased]` and a retroactive `[1.0.0-beta.1]` section reconstructed from existing commits, to document the baseline. The legacy `v2.16.2` git tag is left in place but is **not** part of the new release model.
4. **PR granularity**: single PR closing all related issues atomically (the changes are interdependent).

## Deliverables

### 1. POM — Maven CI Friendly Versions

- Root `pom.xml`:
  - Add `<revision>1.0.0-SNAPSHOT</revision>` to `<properties>`.
  - Replace the literal `<version>1.0.0-SNAPSHOT</version>` of the project with `<version>${revision}</version>`.
  - Add `flatten-maven-plugin` (build/plugins) configured to produce a flattened, deployable POM with `${revision}` resolved (mode `resolveCiFriendliesOnly`, goals `flatten` on `process-resources` and `clean` on `clean`).
  - Add `.flattened-pom.xml` to `.gitignore` if not already present.
- `transforms/parquet-enhanced/pom.xml` and `assemblies/assemblies-parquet-enhanced/pom.xml`:
  - In the `<parent>` block, keep `<version>${revision}</version>` (or remove the explicit version if the flatten plugin handles it — to be verified during implementation; if literal version is required, use `${revision}`).
  - Verify and align any `${plugin.version}` reference in `<finalName>` so that the assembly artifact name resolves to `hop-parquet-plugin-${revision}.zip`. If `plugin.version` is a separate property, point it at `${revision}` (or replace it with `${project.version}`).

Acceptance: `mvn -B -Drevision=9.9.9 clean package -s .m2/settings.xml` produces `assemblies/assemblies-parquet-enhanced/target/hop-parquet-plugin-9.9.9.zip`.

### 2. `.github/workflows/ci.yml` (replaces `pr-build.yml`)

Triggered by `pull_request` on `main` and `push` on `main`. Steps:

1. `actions/checkout@v4`.
2. `actions/setup-java@v4` with `java-version: '17'`, `distribution: 'temurin'`.
3. `mvn -B clean verify -s .m2/settings.xml` with `GITHUB_ACTOR` / `GITHUB_TOKEN` exported.
4. `actions/upload-artifact@v4` conditional on `github.event_name == 'push' && github.ref == 'refs/heads/main'`:
   - `name: snapshot-${{ github.sha }}`
   - `path: assemblies/assemblies-parquet-enhanced/target/hop-parquet-plugin-*.zip`
   - `retention-days: 5`
   - `if-no-files-found: error`

`pr-build.yml` is deleted.

### 3. `.github/workflows/release.yml` (rewritten)

Triggered by tags matching `v[0-9]+.[0-9]+.[0-9]+*`. Steps:

1. Checkout (`actions/checkout@v4`).
2. **Validate SemVer tag** with regex `^v[0-9]+\.[0-9]+\.[0-9]+(-(alpha|beta|rc)\.[0-9]+)?$`; fail with a clear error if not matched.
3. `actions/setup-java@v4`, JDK 17 Temurin.
4. **Get version from tag**: `VERSION=${GITHUB_REF_NAME#v}` exported as a step output.
5. **Build**: `mvn -B -Drevision=${VERSION} clean package -s .m2/settings.xml` (env: `GITHUB_ACTOR`, `GITHUB_TOKEN`).
6. **Extract changelog section**: `awk` on `CHANGELOG.md` matching the `## [VERSION]` heading; fail if missing or empty.
7. **Compute sha256**: `sha256sum hop-parquet-plugin-${VERSION}.zip > hop-parquet-plugin-${VERSION}.zip.sha256` inside `assemblies/assemblies-parquet-enhanced/target`.
8. **Create GitHub Release** with `softprops/action-gh-release@v2`:
   - `files`: zip + sha256
   - `generate_release_notes: true`
   - `prerelease: ${{ contains(github.ref_name, '-') }}`
   - `body`: changelog section extracted in step 6

The previous "tag must be from main" check is removed (not present in the template; tag pushers are trusted maintainers).

### 4. `CHANGELOG.md`

New file at repo root in Keep-a-Changelog 1.1.0 format. Header matches the template (intro paragraph + staged `[Unreleased]` policy explanation). Sections:

- `## [Unreleased]` — empty.
- `## [1.0.0-beta.1] - <implementation-date>` — retroactive, populated from commit history. Categories and indicative entries:
  - **Added**: `ParquetInputEnhanced` and `ParquetOutputEnhanced` transforms (read/write); Files Tab in Parquet Input; unit tests for `ParquetField`, `ParquetRecordMaterializer`, `ParquetWriteSupport` type dispatch and null handling, `ParquetValueConverter`, `ParquetInputStream`, `buildFilename`, `getFields`, `ParquetOutputEnhancedMeta` copy constructor.
  - **Changed**: GitHub Actions upgraded to Node.js 24 compatible versions; alignment with Apache Hop changes from issues #5730 and #5947; README expanded with detailed project description, features, setup.
  - **Fixed**: NPE when using "Accept filenames from previous transform"; shallow copy in `ParquetOutputEnhancedMeta` copy constructor; multiple bugs in `ParquetInputStream`; conversion bugs in `ParquetValueConverter`; `ParquetVersion` fallback and `ParquetOutputStream` position tracking; Fields tab sort error (`ColumnInfo` constructor); file lock caused by VFS `FileObject` not being closed; Cancel marking pipeline as modified.
- Link references at the bottom: `[Unreleased]: .../compare/v1.0.0-beta.1...HEAD` and `[1.0.0-beta.1]: .../releases/tag/v1.0.0-beta.1`.

Note: `v1.0.0-beta.1` is documented as a baseline but **no tag is created retroactively**. The first real tag under the new model will be decided by the maintainer (typically `v1.0.0-beta.2` or `v1.0.0`).

### 5. `docs/guides/tech/release-process.md`

Operator guide adapted from the template, with project-specific paths and artifact names. Contents:

- **Overview** — table of three distribution levels (Snapshot / Pre-release / Release) + Mermaid flow diagram.
- **Prerequisites** — JDK 17, Maven 3.9+, `-s .m2/settings.xml`, `GITHUB_ACTOR` / `GITHUB_TOKEN` with `read:packages`, tag push permission.
- **Snapshot artifacts** — how to download `snapshot-<sha>` from the `CI` workflow run; internal zip name `hop-parquet-plugin-<revision>.zip`.
- **Cutting a release** — Mermaid flowchart + numbered steps: rename `[Unreleased]` to `[X.Y.Z] - YYYY-MM-DD`, recreate empty `[Unreleased]`, update link references, commit `docs: changelog for X.Y.Z` on `main`, tag `vX.Y.Z`, push tag, watch `Release` workflow, bump `<revision>` to `X.Y.(Z+1)-SNAPSHOT` for finals.
- **Cutting a pre-release** — `beta` / `rc` suffixes, iteration of `.N`, no `<revision>` bump until GA.
- **Verifying locally** — `mvn -B -Drevision=X.Y.Z clean package -s .m2/settings.xml`; check `assemblies/assemblies-parquet-enhanced/target/hop-parquet-plugin-X.Y.Z.zip`.
- **Hotfix release** — branch from past tag, fix, changelog, tag, cherry-pick to `main`.
- **Rollback / yank** — never delete a release; ship a follow-up patch.
- **Troubleshooting** — SemVer validation error, missing changelog section, tag on wrong commit, `.flattened-pom.xml`, `org.apache.hop:*` resolution failures.
- **Reference** — links to `pom.xml`, `.github/workflows/ci.yml`, `.github/workflows/release.yml`, `CHANGELOG.md`, `.m2/settings.xml`.

### 6. `CLAUDE.md` updates

Replace the current minimal "Tools and References" section with the template's expanded version:

- **context7**: always use it for library/framework docs (already present, keep).
- **Serena — symbolic-first**: prefer `find_symbol`, `get_symbols_overview`, `find_referencing_symbols`, `replace_symbol_body`, `insert_before_symbol`, `insert_after_symbol` over `Read` of whole files. Use `Read` only for small files (< ~50 lines), configuration, or non-code (Markdown, YAML, properties).
- **Session start**: activate Serena on this project (`activate_project` with the repository path) and verify onboarding (`check_onboarding_performed`; if `false`, run `onboarding`). Once per session, before any code task.
- **Subagent dispatch**: when dispatching subagents on code tasks, the Serena directive MUST be repeated verbatim inside the dispatch prompt — subagents do not see `CLAUDE.md`.
- **Cross-project Serena lookup on `hop-serasoft`**:
  - Switch with `activate_project("hop-serasoft")` → run `find_*` / `get_*` → switch back with `activate_project("<this-project>")` **before** any modification.
  - `hop-serasoft` is **read-only**: never use `replace_symbol_body`, `insert_before_symbol`, `insert_after_symbol`, `safe_delete_symbol`, or any write tool while `hop-serasoft` is the active project.
  - Subagent dispatch: the activate-query-deactivate cycle and read-only constraint MUST be spelled out in the dispatch prompt.

Add to the Task Completion Checklist:

> "If the PR introduces a user-visible change (feature, behaviour change, bug fix, removal, deprecation, security fix), add an entry under `[Unreleased]` in `CHANGELOG.md` in the appropriate category. Pure refactors, internal tests, CI tweaks and similar invisible changes do not require an entry."

## GitHub Issues mapping

Per `CLAUDE.md`, every plan item maps to a GitHub Issue. Six issues are created and all closed by a single PR via closing keywords in the PR body:

1. Adopt Maven CI Friendly Versions (`${revision}`) and `flatten-maven-plugin` in POMs.
2. Replace `pr-build.yml` with `ci.yml` aligned to the template.
3. Rewrite `release.yml` with SemVer validation, changelog extraction, sha256, automatic prerelease detection.
4. Add `CHANGELOG.md` (Keep-a-Changelog 1.1.0) with `[Unreleased]` and retroactive `[1.0.0-beta.1]`.
5. Add release process guide at `docs/guides/tech/release-process.md`.
6. Update `CLAUDE.md` with expanded Serena rules and changelog-entry checklist item.

PR strategy: **one single PR**, dedicated branch (e.g. `chore/release-management-alignment`), body with `Fixes #N` for all six issues.

## Out of scope

- Migration of the legacy `v2.16.2` tag/release: left in place untouched.
- Publication of Maven artifacts to GitHub Packages or Maven Central: not part of the template flow.
- Signing of release assets (GPG / cosign): not part of the template flow.
- Automated bump of `<revision>` after release: kept manual, as in the template.

## Acceptance criteria

- `mvn -B -Drevision=1.2.3 clean verify -s .m2/settings.xml` succeeds and produces `hop-parquet-plugin-1.2.3.zip` under `assemblies/assemblies-parquet-enhanced/target/`.
- A push to `main` after merge triggers `CI` and uploads a `snapshot-<sha>` artifact.
- Pushing a tag `v1.0.0-beta.2` (after CHANGELOG section is added) triggers `Release`, the workflow validates the tag, builds with `-Drevision=1.0.0-beta.2`, extracts the matching changelog section, computes sha256, and publishes a GitHub pre-release containing both `.zip` and `.zip.sha256`.
- Pushing a malformed tag (e.g. `v1.0`, `v1.0.0-foo`) fails at the `Validate SemVer tag` step.
- `CHANGELOG.md` exists at repo root with `[Unreleased]` and `[1.0.0-beta.1]` sections.
- `docs/guides/tech/release-process.md` exists and matches the template's structure.
- `CLAUDE.md` contains the expanded Serena rules and the changelog-entry checklist item.
