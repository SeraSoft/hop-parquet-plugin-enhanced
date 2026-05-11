# CLAUDE.md — hop-parquet-plugin-enhanced


## Project Overview

Apache Hop plugin that provides enhanced Parquet file input/output transforms, replacing the standard Parquet plugin bundled with Hop.

- **Two transforms**: `ParquetInputEnhanced` (read) and `ParquetOutputEnhanced` (write)
- **Target platform**: Apache Hop 2.16.2+
- **License**: Apache 2.0 (ASF header required on all Java files)

## Versioning policy

- Project version and Hop version are decoupled. Release tags follow the project version (e.g. `v1.0.0`), not the Hop version.
- The project version is carried by the `<revision>` property in the root `pom.xml` (Maven CI-friendly versions); every reactor module references it via `${revision}`. `flatten-maven-plugin` produces a flattened POM at `package` so published artifacts contain the resolved version. The release workflow overrides `-Drevision=<tag-version>` at build time.
- On `main`, `<revision>` is `X.Y.Z-SNAPSHOT`. After cutting a final `vX.Y.Z` release, `main` is bumped to `X.Y.(Z+1)-SNAPSHOT`. After a pre-release (`vX.Y.Z-{beta,rc}.N`), `<revision>` is **not** bumped — it stays at `X.Y.Z-SNAPSHOT` until the final `X.Y.Z` ships.
- To upgrade the Hop version used for compilation, bump the `hop.version` Maven property and (if needed) the `<parent>` version in the root `pom.xml`. The project version does not have to change as a consequence.

## Tech Stack

| Component       | Version  |
|-----------------|----------|
| Java (source)   | 11       |
| Java (CI)       | 17       |
| Apache Hop      | 2.16.2   |
| Apache Parquet  | 1.15.2   |
| Hadoop Client   | 3.3.6    |
| Lombok          | 1.18.34  |
| Build           | Maven    |

## Module Structure

```
pom.xml                          # Parent POM (packaging: pom)
transforms/
  parquet-enhanced/              # Main code module (parent: hop:2.16.2)
    src/main/java/org/apache/hop/parquet/transforms/
      input/                     # ParquetInputEnhanced (13 classes)
      output/                    # ParquetOutputEnhanced (11 classes)
    src/main/resources/          # i18n messages, SVG icons
assemblies/
  assemblies-parquet-enhanced/   # Assembly for distribution zip
```

## Build Commands

**Always use `-s .m2/settings.xml`** — custom Maven settings referencing GitHub Packages for Hop artifacts.

```bash
# Full build
mvn clean package -s .m2/settings.xml

# Verify (CI uses this)
mvn clean verify -s .m2/settings.xml

# Compile only
mvn clean compile -s .m2/settings.xml
```

Requires `GITHUB_ACTOR` and `GITHUB_TOKEN` environment variables for GitHub Packages authentication.

## Code Conventions

### Apache Hop Transform Pattern
Every transform consists of 3-4 classes:
- `*Meta` — metadata/configuration, annotated with `@Transform`, extends `BaseTransformMeta<T, D>`
- `*Data` — runtime data holder, extends `BaseTransformData`
- Main class (no suffix) — execution logic, extends `BaseTransform<Meta, Data>`
- `*Dialog` — SWT UI dialog for Hop GUI

### Lifecycle: `init()` -> `processRow()` -> `dispose()`

### Annotations
- `@Transform(id, image, name, description, ...)` — transform registration
- `@HopMetadataProperty(key, groupKey, injectionKeyDescription)` — field serialization
- `@Getter` / `@Setter` (Lombok) — used on all Meta fields
- Jandex plugin indexes annotations at build time

### i18n
- Pattern: `BaseMessages.getString(PKG, "key")`
- Messages in `messages_en_US.properties` and `messages_it_IT.properties`
- All user-facing strings must be in both locales

### File Access
Uses Apache Commons VFS (`HopVfs`) for filesystem abstraction. Files read entirely into memory before Parquet parsing.

## Task Completion Checklist

1. Build passes: `mvn clean verify -s .m2/settings.xml`
2. ASF 2.0 license header on all new Java files
3. New strings added to both `messages_en_US.properties` and `messages_it_IT.properties`
4. Lombok `@Getter`/`@Setter` on Meta fields, `@HopMetadataProperty` for serialization
5. New runtime dependencies added to `assembly.xml`
6. No unit tests exist yet — consider adding them for new logic
7. If the PR introduces a user-visible change (feature, behaviour change, bug fix, removal, deprecation, security fix), add an entry under `[Unreleased]` in `CHANGELOG.md` in the appropriate category. Pure refactors, internal tests, CI tweaks and similar invisible changes do not require an entry.
