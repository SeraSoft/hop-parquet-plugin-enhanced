# Project Overview

**hop-parquet-plugin-enhanced** is an Apache Hop plugin providing enhanced Parquet file input/output transforms.

## Purpose
Enhanced replacement for the standard Parquet plugin bundled with Apache Hop. Provides two transforms:
- **ParquetInputEnhanced** (id: `ParquetFileInputEnhanced`) — reads Parquet files
- **ParquetOutputEnhanced** (id: `ParquetFileEnhancedOutput`) — writes Parquet files

## Tech Stack
- Java 11 (source/target)
- Apache Hop 2.16.2 (parent POM)
- Apache Parquet 1.15.2
- Apache Hadoop Client 3.3.6
- Lombok 1.18.34
- SWT (Eclipse) for UI dialogs
- Maven multi-module build
- Jandex for annotation indexing
- CI runs on JDK 17 (Temurin)

## Module Structure
```
pom.xml (parent: hop-parquet-plugin, packaging: pom)
├── transforms/
│   └── parquet-enhanced/    (main code module, parent: hop 2.16.2)
│       └── src/main/java/org/apache/hop/parquet/transforms/
│           ├── input/       (13 classes)
│           └── output/      (11 classes)
├── assemblies/
│   └── assemblies-parquet-enhanced/   (assembly for distribution zip)
```

## Distribution
Assembly produces a zip file at `assemblies/assemblies-parquet-enhanced/target/hop-parquet-plugin-*.zip` that gets deployed into `plugins/tech/parquet-enhanced` inside a Hop installation.

## i18n
Messages in `messages_en_US.properties` and `messages_it_IT.properties` for both input and output transforms.
