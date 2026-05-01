# Task Completion Checklist

When a development task is completed, verify the following:

1. **Build**: Run `mvn clean verify -s .m2/settings.xml` — must pass without errors
2. **License headers**: All new Java files must include the ASF 2.0 license header
3. **i18n**: Any new user-facing strings must be added to both `messages_en_US.properties` and `messages_it_IT.properties`
4. **Lombok**: Use `@Getter`/`@Setter` consistently on metadata fields
5. **HopMetadataProperty**: All serialized fields in Meta classes must be annotated with `@HopMetadataProperty`
6. **Jandex**: The build includes the jandex-maven-plugin for annotation indexing — ensure new `@Transform` classes are picked up
7. **Assembly**: If new runtime dependencies are added, update `assemblies/assemblies-parquet-enhanced/src/assembly/assembly.xml`
8. **CHANGELOG**: If the change is user-visible (feature, behaviour change, bug fix, removal, deprecation, security fix), add an entry under `[Unreleased]` in `CHANGELOG.md` in the appropriate category. Pure refactors, internal tests, and CI tweaks do not require an entry.
9. **No tests currently exist** — consider adding JUnit tests for new logic

## Git/PR Workflow
- Every code change goes on a dedicated branch, merged into `main` via PR.
- Every plan item must be mapped to a GitHub Issue.
- PR body must include closing keywords (`Fixes #N`, `Closes #N`) for related issues.
- Claude creates PRs; the user merges them.
- No co-author trailers in commits or PR descriptions.
- Code/commits/PRs in English; GitHub Issues in Italian; working sessions in Italian.
