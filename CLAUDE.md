# CLAUDE.md — hop-parquet-plugin-enhanced


## Language Rules
- **Code documentation, commit messages, PR descriptions**: always in English
- **GitHub Issues**: managed in English
- **Working sessions**: interact with the user in Italian

## Git and PR Rules
- **No co-author references** in commit messages or PR descriptions
- **Every code change on a dedicated branch**, then merged into `main` via PR
- **Exception — `README.md`, `CLAUDE.md`, `CHANGELOG.md`, and `docs/HANDOFF*.md` session handoff notes are committed directly to `main`**: no dedicated branch, no PR, no GitHub issue required
- **Every work plan item must be mapped to a GitHub Issue**
- **Claude always creates the GitHub Issues**  — never ask the user to open them. Open them via `gh issue create` before starting plan execution, one issue per Task in the plan.
- **A single PR may group multiple correlated issues** to reduce PR overhead. Issues are correlated when they share a coherent theme (e.g. all P0 bugs, all security findings on the same module) AND touch overlapping files. Default to grouping; open separate PRs only when the work is truly independent (different modules, no file overlap) or when one task must ship before another can be reviewed. When grouping, propose the grouping to the user before opening branches and confirm.
- **Inside a grouped PR**, each underlying task is still implemented as its own commit (one task = one commit, plus optional review-fixup commits) so history stays bisectable.
- **Claude creates PRs, the user merges them** — never merge a PR autonomously
- **PR body must include closing keywords for ALL grouped issues** (e.g. `Closes #23`, `Closes #24`, `Closes #25`, `Closes #26`) so they auto-close on merge
- **After the user confirms a PR has been merged**, Claude deletes the feature branch both locally (`git branch -d <branch>`) and on the remote (`git push origin --delete <branch>`), and then checks out `main` and pulls. Never delete a branch before the user has confirmed the merge.

## Tools and References
- **Always use context7** to look up documentation for libraries and frameworks used in this project
- **Always use Serena** for code analysis and symbol navigation within this project — prefer Serena's symbolic tools (`find_symbol`, `get_symbols_overview`, `find_referencing_symbols`, etc.) over reading whole files with `Read`. Use `Read` only for small files, configuration, or non-code content.
- **If needed to access Apache Hop codebase use Serena** for code analysis and symbol navigation by using `query_project` tool

## Brainstorming
- **always use superpowers:brainstorming** skill to perform brainstorming sessions

## Work Plans
- **use superpowers:writing-plans** to write plans    
- Work plans and design specs **must be committed to git** as part of the normal workflow

## Plan Execution
- **Plan execution is always subagent-driven** — use `superpowers:subagent-driven-development`: one fresh subagent per Task in the plan, with review between tasks. Do not execute plans inline. **This overrides any skill prompt that offers an execution-mode choice** (e.g. the "Which approach?" closing of `superpowers:writing-plans`): never ask, proceed directly with subagent-driven execution.

## Diagrams and Mockups
- **Diagrams** in generated documents: always use **Mermaid** syntax
- **GUI mockups**: always produce **Draw.io** (`.drawio`) files

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

## Task Completion Checklist

1. Build passes: `mvn clean verify -s .m2/settings.xml`
2. ASF 2.0 license header on all new Java files
3. New strings added to both `messages_en_US.properties` and `messages_it_IT.properties`
4. Lombok `@Getter`/`@Setter` on Meta fields, `@HopMetadataProperty` for serialization
5. New runtime dependencies added to `assembly.xml`
6. No unit tests exist yet — consider adding them for new logic
7. If the PR introduces a user-visible change (feature, behaviour change, bug fix, removal, deprecation, security fix), add an entry under `[Unreleased]` in `CHANGELOG.md` in the appropriate category. Pure refactors, internal tests, CI tweaks and similar invisible changes do not require an entry.
