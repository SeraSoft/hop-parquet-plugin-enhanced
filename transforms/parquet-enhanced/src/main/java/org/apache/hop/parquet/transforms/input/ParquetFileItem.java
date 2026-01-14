package org.apache.hop.parquet.transforms.input;

import lombok.Getter;
import lombok.Setter;
import org.apache.hop.metadata.api.HopMetadataProperty;

public class ParquetFileItem {

  /** Array of filenames */
  @HopMetadataProperty @Getter @Setter private String fileName;

  /** Wildcard or filemask (regular expression) */
  @HopMetadataProperty @Getter @Setter private String fileMask;

  /** Wildcard or filemask to exclude (regular expression) */
  @HopMetadataProperty @Getter @Setter private String excludeFileMask;

  /** Array of boolean values as string, indicating if a file is required. */
  @HopMetadataProperty @Getter @Setter private String fileRequired;

  /** Array of boolean values as string, indicating if we need to fetch sub folders. */
  @HopMetadataProperty @Getter @Setter private String includeSubFolders;

  public ParquetFileItem() {}
}
