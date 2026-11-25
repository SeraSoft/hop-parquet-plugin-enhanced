# CLAUDE.md — hop-parquet-plugin-enhanced

Generic Rules 1–16 come from the user-level layers: `~/.agent-dotfiles/AGENTS.md` (universal) plus the dev profile imported via the untracked `CLAUDE.local.md` (same numbering; Rule 6 retired). This file holds project-specific guidance only.

## Project Overview

Apache Hop plugin that provides enhanced Parquet file input/output transforms, replacing the standard Parquet plugin bundled with Hop.

- **License**: Apache 2.0 (ASF header required on all Java files)

## Versioning policy

- Project version and Hop version are decoupled. Release tags follow the project version (e.g. `v1.0.0`), not the Hop version.
- The project version is carried by the `<revision>` property in the root `pom.xml` (Maven CI-friendly versions); every reactor module references it via `${revision}`. `flatten-maven-plugin` produces a flattened POM at `package` so published artifacts contain the resolved version. The release workflow overrides `-Drevision=<tag-version>` at build time.
- On `main`, `<revision>` is `X.Y.Z-SNAPSHOT`. After cutting a final `vX.Y.Z` release, `main` is bumped to `X.Y.(Z+1)-SNAPSHOT`. After a pre-release (`vX.Y.Z-{beta,rc}.N`), `<revision>` is **not** bumped — it stays at `X.Y.Z-SNAPSHOT` until the final `X.Y.Z` ships.
- To upgrade the Hop version used for compilation, bump the `hop.version` Maven property and (if needed) the `<parent>` version in the root `pom.xml`. The project version does not have to change as a consequence.

## Build

- Always pass `-s .m2/settings.xml`: the custom Maven settings point to GitHub Packages, where the Hop artifacts are resolved.
- `GITHUB_ACTOR` and `GITHUB_TOKEN` environment variables are required for GitHub Packages authentication.
- Java source/target level is 11; CI builds with JDK 17.

## Code Conventions

- `@Getter`/`@Setter` (Lombok) on all Meta fields; `@HopMetadataProperty` for serialization.
- All user-facing strings go in both `messages_en_US.properties` and `messages_it_IT.properties`.
- File access goes through VFS (`HopVfs`). Files are read entirely into memory before Parquet parsing.

## Task Completion Checklist

1. Build passes: `mvn clean verify -s .m2/settings.xml`
2. ASF 2.0 license header on all new Java files
3. New strings added to both `messages_en_US.properties` and `messages_it_IT.properties`
4. New runtime dependencies added to `assemblies/assemblies-parquet-enhanced/src/assembly/assembly.xml`
5. If the PR introduces a user-visible change (feature, behaviour change, bug fix, removal, deprecation, security fix), add an entry under `[Unreleased]` in `CHANGELOG.md` in the appropriate category. Pure refactors, internal tests, CI tweaks and similar invisible changes do not require an entry.
