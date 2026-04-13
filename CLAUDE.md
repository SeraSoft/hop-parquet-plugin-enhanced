# CLAUDE.md — hop-parquet-plugin-enhanced

## Project Overview

Apache Hop plugin that provides enhanced Parquet file input/output transforms, replacing the standard Parquet plugin bundled with Hop.

- **Two transforms**: `ParquetInputEnhanced` (read) and `ParquetOutputEnhanced` (write)
- **Target platform**: Apache Hop 2.16.2+
- **License**: Apache 2.0 (ASF header required on all Java files)

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

## CI/CD

- **PR builds** (`.github/workflows/pr-build.yml`): `mvn clean verify` on ubuntu-latest, JDK 17
- **Releases** (`.github/workflows/release.yml`): triggered by `v*` tags from `main`, creates GitHub Release with assembly zip

## Distribution

The assembly produces `assemblies/assemblies-parquet-enhanced/target/hop-parquet-plugin-*.zip`.
Install by extracting into `plugins/tech/parquet-enhanced/` inside a Hop installation.

## Language Rules

- **Code documentation, commit messages, PR descriptions**: always in English
- **GitHub Issues**: managed in Italian
- **Working sessions**: interact with the user in Italian

## Git and PR Rules

- **No co-author references** in commit messages or PR descriptions

## Diagrams and Mockups

- **Diagrams** in generated documents: always use **Mermaid** syntax
- **GUI mockups**: always produce **Draw.io** (`.drawio`) files

## Task Completion Checklist

1. Build passes: `mvn clean verify -s .m2/settings.xml`
2. ASF 2.0 license header on all new Java files
3. New strings added to both `messages_en_US.properties` and `messages_it_IT.properties`
4. Lombok `@Getter`/`@Setter` on Meta fields, `@HopMetadataProperty` for serialization
5. New runtime dependencies added to `assembly.xml`
6. No unit tests exist yet — consider adding them for new logic
