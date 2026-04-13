# Apache Hop Enhanced Parquet Plugin

## Overview

This plugin provides enhanced [Apache Parquet](https://parquet.apache.org/) read/write transforms for [Apache Hop](https://hop.apache.org), replacing the standard Parquet plugin bundled with Hop.

### Transforms included

| Transform | Category | Description |
|-----------|----------|-------------|
| **Enhanced Parquet File Input** | Input | Reads data from Parquet files with support for automatic schema detection, field mapping, type conversion, and file metadata extraction |
| **Enhanced Parquet File Output** | Output | Writes data to Parquet files with configurable compression, Parquet version selection, row group sizing, and flexible filename generation |

### Key features

- Automatic schema extraction from Parquet file metadata
- Support for all major Parquet types: INT32, INT64, INT96, FLOAT, DOUBLE, BOOLEAN, BINARY
- Logical type handling: Timestamp, Date, Time, Decimal, JSON
- Parquet 1.0 and 2.0 format support
- Compression codecs: Uncompressed, Snappy, GZip, LZO, Brotli, LZ4, ZSTD
- Accept filenames from previous transforms for dynamic file processing
- File metadata fields: path, size, extension, hidden flag, last modification time, URI
- Configurable row group and page sizes for write performance tuning

## System Requirements

- [Apache Hop](https://hop.apache.org) **2.16.2** or above
- Java 11+ (runtime)

## Installation

Download the latest release zip from the [Releases](https://github.com/SeraSoft/hop-parquet-plugin-enhanced/releases) page, then:

1. Copy the zip file into the root directory of your Apache Hop installation
2. Unzip it from there — the archive structure will place the plugin files in the correct location automatically:
   ```bash
   cd /path/to/hop
   unzip hop-parquet-plugin-2.16.2.zip
   ```
3. Restart Hop

The transforms will appear as **Enhanced Parquet File Input** and **Enhanced Parquet File Output** in the transform palette.

## Building from source

### Prerequisites

- JDK 17
- Maven 3.8+
- A GitHub account with access to the [SeraSoft/hop](https://github.com/SeraSoft/hop) Maven packages

### Authentication setup

The build requires Apache Hop artifacts published on GitHub Packages. Set the following environment variables:

```bash
export GITHUB_ACTOR=<your-github-username>
export GITHUB_TOKEN=<your-github-personal-access-token>
```

The token needs the `read:packages` scope.

### Build commands

All Maven commands must use the custom settings file:

```bash
# Full build (compile + test + package)
mvn clean package -s .m2/settings.xml

# Run tests only
mvn clean verify -s .m2/settings.xml

# Skip tests (faster build)
mvn clean package -s .m2/settings.xml -DskipTests
```

The distributable zip is generated at:
```
assemblies/assemblies-parquet-enhanced/target/hop-parquet-plugin-2.16.2.zip
```

### Project structure

```
pom.xml                                 # Parent POM
transforms/
  parquet-enhanced/                     # Plugin source code
    src/main/java/.../transforms/
      input/                            # Enhanced Parquet File Input
      output/                           # Enhanced Parquet File Output
    src/test/java/                      # Unit tests (JUnit 5)
    src/main/resources/                 # i18n messages, SVG icons
assemblies/
  assemblies-parquet-enhanced/          # Assembly descriptor for distribution zip
.m2/
  settings.xml                          # Maven settings for GitHub Packages
.github/workflows/
  pr-build.yml                          # CI: build + test on pull requests
  release.yml                           # CD: build + publish on version tags
```

## CI/CD

- **Pull requests**: automatically built and tested on `ubuntu-latest` with JDK 17
- **Releases**: triggered by pushing a `v*` tag from the `main` branch. Creates a GitHub Release with the assembly zip attached.

## Contributing

Contributions are welcome. Please open an issue to discuss proposed changes before submitting a pull request.

- Every change should be on a dedicated branch, merged via PR
- Code, commit messages, and PR descriptions must be in English
- GitHub Issues are managed in Italian

## Support

This plugin is provided as is, without any warranties, expressed or implied. This software is not covered by any Support Agreement.

## License

Licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
