# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/) and this project adheres to [Semantic Versioning 2.0.0](https://semver.org/).

Under `[Unreleased]`, every pull request that introduces a user-visible change adds an entry to the appropriate category (`Added`, `Changed`, `Fixed`, `Removed`, `Deprecated`, `Security`). When a release is cut, the release PR renames `[Unreleased]` to `[<version>] - YYYY-MM-DD` and creates a new empty `[Unreleased]` section above it.

## [Unreleased]

### Fixed

- Parquet input file remained locked after using "Get fields" in the dialog: `ParquetInputEnhancedMeta.extractRowMeta` now closes the VFS `FileObject`, the input stream and the `ParquetReader` deterministically via try-with-resources (#22).

## [1.0.0-beta.1] - 2026-05-01

### Added

- `ParquetInputEnhanced` transform: reads one or more Parquet files via Apache Commons VFS, with a Files tab supporting explicit file lists and "Accept filenames from previous transform" mode.
- `ParquetOutputEnhanced` transform: writes Parquet files with configurable compression and schema derived from the incoming row metadata.
- Unit tests for `ParquetField`, `ParquetRecordMaterializer`, `ParquetWriteSupport` (type dispatch and null handling), `ParquetValueConverter`, `ParquetInputStream`, `buildFilename`, `getFields`, and `ParquetOutputEnhancedMeta` copy constructor.

### Changed

- README expanded with detailed project description, features, and setup instructions.
- GitHub Actions upgraded to Node.js 24 compatible versions.
- Aligned with upstream Apache Hop changes from issues #5730 and #5947.

### Fixed

- NPE when "Accept filenames from previous transform" was used in `ParquetInputEnhanced`.
- Shallow copy bug in `ParquetOutputEnhancedMeta` copy constructor.
- Multiple bugs in `ParquetInputStream` surfaced by the new unit tests.
- Conversion bugs in `ParquetValueConverter`.
- `ParquetVersion` fallback path and `ParquetOutputStream` position tracking.
- Fields tab sort error caused by an incorrect `ColumnInfo` constructor invocation.
- File lock on Windows caused by VFS `FileObject` not being closed after reading the file into memory.
- Cancel on the dialog marking the pipeline as modified, by saving and restoring the changed state.

[Unreleased]: https://github.com/SeraSoft/hop-parquet-plugin-enhanced/compare/v1.0.0-beta.1...HEAD
[1.0.0-beta.1]: https://github.com/SeraSoft/hop-parquet-plugin-enhanced/releases/tag/v1.0.0-beta.1
