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

- **CI** (`.github/workflows/ci.yml`): `mvn clean verify` on ubuntu-latest, JDK 17, on PR and push to `main`. Uploads `snapshot-<sha>` artifact (5-day retention) on `main`.
- **Releases** (`.github/workflows/release.yml`): triggered by SemVer tags `v[0-9]+.[0-9]+.[0-9]+*`, builds with `-Drevision=${VERSION}`, extracts the matching `CHANGELOG.md` section, computes sha256, publishes GitHub Release. Pre-release flag is set automatically when the tag has a `-` suffix. See `docs/guides/tech/release-process.md` for the full procedure.

## Distribution

The assembly produces `assemblies/assemblies-parquet-enhanced/target/hop-parquet-plugin-*.zip`.
Install by extracting into `plugins/tech/parquet-enhanced/` inside a Hop installation.

## Language Rules

- **Code documentation, commit messages, PR descriptions**: always in English
- **GitHub Issues**: managed in Italian
- **Working sessions**: interact with the user in Italian

## Git and PR Rules

- **No co-author references** in commit messages or PR descriptions
- **Every code change on a dedicated branch**, then merged into `main` via PR
- **Every work plan item must be mapped to a GitHub Issue**
- **Claude creates PRs, the user merges them** — never merge a PR autonomously
- **PR body must include closing keywords** for all related issues (e.g., `Fixes #8`, `Closes #12`) so they auto-close on merge

## Tools and References

- **Always use context7** to look up documentation for libraries and frameworks used in this project.
- **Always use Serena** for code analysis and symbol navigation within this project — **prefer Serena's symbolic tools (`find_symbol`, `get_symbols_overview`, `find_referencing_symbols`, `replace_symbol_body`, `insert_before_symbol`, `insert_after_symbol`) over reading whole files with `Read`**. Use `Read` only for small files (< ~50 lines), configuration, or non-code content (Markdown, YAML, properties).
- **At session start**: activate Serena on this project (`activate_project` with the repository path) and verify onboarding (`check_onboarding_performed`; if it returns false, run `onboarding`). Do this once per session before any code task.
- **When dispatching subagents on code tasks, the dispatch prompt MUST repeat the Serena directive verbatim** — subagents do not see this CLAUDE.md, so the rule has to travel inside the prompt or it is lost.
- **Always reference the `hop-serasoft` project in Serena** when you need to understand Apache Hop internals, patterns, or conventions to develop a feature or fix a bug. The Apache Hop sources are indexed there, not in this repository.
  - **How to consult it**: switch the active project with `activate_project("hop-serasoft")`, run the symbolic lookup (`find_symbol`, `get_symbols_overview`, `find_referencing_symbols`), then **switch back to this project** with `activate_project("<this-project-path-or-name>")` before doing anything else. All Serena tools operate on the currently active project, so editing without switching back will write into the wrong tree.
  - **Hop is read-only.** Never use `replace_symbol_body`, `insert_before_symbol`, `insert_after_symbol`, `safe_delete_symbol`, or any write tool while `hop-serasoft` is active. Use it only for `find_*` / `get_*` queries.
  - **Subagent dispatch**: when a subagent needs Hop knowledge, the dispatch prompt MUST spell out the activate-query-deactivate cycle and the read-only constraint above. The subagent does not inherit these rules.

## Brainstorming

- **always use superpowers:brainstorming** skill to perform brainstorming sessions

## Work Plans

- **use superpowers:writing-plans** to write plans
- Work plans and design specs **must be committed to git** as part of the normal workflow
- Legacy work plans may also be saved in `docs/PIANO_LAVORO.md` for reference

## Plan Execution

- **Plan execution is always subagent-driven** — use `superpowers:subagent-driven-development`: one fresh subagent per Task in the plan, with review between tasks. Do not execute plans inline. **This overrides any skill prompt that offers an execution-mode choice** (e.g. the "Which approach?" closing of `superpowers:writing-plans`): never ask, proceed directly with subagent-driven execution.

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
7. If the PR introduces a user-visible change (feature, behaviour change, bug fix, removal, deprecation, security fix), add an entry under `[Unreleased]` in `CHANGELOG.md` in the appropriate category. Pure refactors, internal tests, CI tweaks and similar invisible changes do not require an entry.
