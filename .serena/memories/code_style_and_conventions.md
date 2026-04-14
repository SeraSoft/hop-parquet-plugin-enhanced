# Code Style and Conventions

## General
- **License header**: Apache 2.0 license header on every Java file (ASF standard)
- **Checkstyle**: Explicitly skipped (`<checkstyle.skip>true</checkstyle.skip>`)
- **Formatting**: Google Java Style-like (used by Apache Hop upstream)
  - 2-space indentation in POM files
  - 4-space indentation in Java (appears consistent with Hop conventions)
  - Line wrapping for long method signatures

## Naming
- Classes follow Apache Hop transform conventions:
  - `*Meta` — metadata/configuration (annotated with `@Transform`)
  - `*Data` — runtime data holder
  - `*Dialog` — SWT UI dialog
  - Main transform class (no suffix) — execution logic
- Package: `org.apache.hop.parquet.transforms.{input,output}`
- Constants: `UPPER_SNAKE_CASE`

## Annotations & Patterns
- **Lombok**: `@Getter`, `@Setter` used extensively on fields (avoids boilerplate)
- **Hop Metadata**: `@HopMetadataProperty` for serialization (with `key`, `groupKey`, `injectionKeyDescription`)
- **Transform Registration**: `@Transform` annotation with `id`, `image`, `name`, `description`, `categoryDescription`, `documentationUrl`, `keywords`
- **i18n**: `BaseMessages.getString(PKG, "key")` pattern, with `PKG = <MetaClass>.class`

## Hop Transform Lifecycle
- `init()` → initialization, file validation
- `processRow()` → main processing loop
- `dispose()` → cleanup
- Meta classes extend `BaseTransformMeta<Transform, Data>`
- Transform classes extend `BaseTransform<Meta, Data>`

## VFS
The plugin uses Apache Commons VFS (`HopVfs`) for file access, supporting various filesystems.
Files are read entirely into memory via `ByteArrayOutputStream` before parsing with Parquet.
