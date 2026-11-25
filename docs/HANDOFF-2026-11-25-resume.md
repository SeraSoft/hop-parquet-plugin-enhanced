# HANDOFF — 2026-11-25 — resume notes for when development restarts

Development on this plugin is paused. These notes record the open points found during the agent-dotfiles migration, to pick up when work resumes.

## Done on 2026-11-25

- Migrated to the agent-dotfiles layers: untracked `CLAUDE.local.md` imports the dev profile and `languages/java.md`; `CLAUDE.md` now holds project-specific guidance only.
- Removed the tracked Claude memory files under `.claude/projects/` (stale session state and preferences now covered by the user-level layers).

## Work in progress

- Local branch `fix/39-propagate-previous-fields` (issue #39): 3 commits, not pushed, no PR. It branches from an older `main`; rebase it on `main` before opening the PR.
- The design spec and implementation plan for #39 are on `main` under `docs/superpowers/`.
- `.serena/project.yml` has an uncommitted local change on that branch.

## Release and versioning inconsistencies (fix when development restarts)

1. Tag `v2.16.2` follows the Hop version, not the project version, which contradicts the versioning policy in `CLAUDE.md`. It has a GitHub Release currently marked **Latest**, while the real project release is the pre-release `v1.0.0-beta.1`. Decide whether to keep, rename or mark it as legacy.
2. Hadoop version drift: root `pom.xml` has `hadoop.version` 3.4.1; `transforms/parquet-enhanced/pom.xml` has 3.3.6 (and `CLAUDE.md` Tech Stack says 3.3.6). Pick one and keep it in the root POM only.
3. The module POM `transforms/parquet-enhanced/pom.xml` redefines `revision`, `hop.version` and Lombok properties already set in the root POM; remove the duplicates so they cannot drift.
4. `CLAUDE.md` has no CI/CD section and no pointer to `docs/guides/tech/release-process.md`, unlike the sibling Hop plugins (`hop-sqlserver-bulk-loader`, `hop-opentelemetry-plugin`).
5. `docs/guides/tech/release-process.md` always bumps to the next patch SNAPSHOT after a final release. `hop-sqlserver-bulk-loader` now uses a richer rule (`docs/guides/tech/release-process.md` § "Choosing the next version": patch by default, minor when planned, the CHANGELOG content decides before tagging). Consider aligning this plugin to it.
6. Stale remote-tracking ref `origin/chore/release-management-alignment`: the branch is gone on the remote; run `git fetch --prune`.

## Notes

- `<revision>` on `main` is still `1.0.0-SNAPSHOT` after `v1.0.0-beta.1`: this is correct (no bump after a pre-release).
- Hop plugins keep their own release policy; the company-wide strategy in agent-dotfiles issue #29 covers company projects only.
