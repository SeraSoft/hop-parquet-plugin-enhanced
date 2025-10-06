/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.parquet.transforms.input;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.vfs2.FileObject;
import org.apache.hop.core.annotations.Transform;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.fileinput.FileInputList;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.TransformMeta;

@Transform(
    id = "ParquetFileInputEnhanced",
    image = "parquet_input.svg",
    name = "i18n::ParquetInput.Name",
    description = "i18n::ParquetInput.Description",
    categoryDescription = "i18n:org.apache.hop.pipeline.transform:BaseTransform.Category.Input",
    documentationUrl = "/pipeline/transforms/parquet-file-input.html",
    keywords = "i18n::ParquetInputMeta.keyword")
public class ParquetInputEnhancedMeta
    extends BaseTransformMeta<ParquetInputEnhanced, ParquetInputEnhancedData> {

  public static final Class<?> PKG = ParquetInputEnhancedMeta.class;

  public static final String[] RequiredFilesDesc =
      new String[] {
        BaseMessages.getString(PKG, "System.Combo.No"),
        BaseMessages.getString(PKG, "System.Combo.Yes")
      };

  /** Are we accepting filenames in input rows? */
  @HopMetadataProperty @Getter @Setter private boolean acceptingFilenames;

  /** If receiving input rows, should we pass through existing fields? */
  @Getter @Setter @HopMetadataProperty private boolean passingThruFields;

  /** The field in which the filename is placed */
  @Getter @Setter @HopMetadataProperty private String acceptingField;

  /** The transformName to accept filenames from */
  @Getter @Setter @HopMetadataProperty private String acceptingTransformName;

  /** The transform to accept filenames from */
  @Getter @Setter private TransformMeta acceptingTransform;

  @HopMetadataProperty(
      key = "fileEntry",
      groupKey = "fileEntries")
  @Getter
  @Setter
  private List<ParquetFileItem> fileItems;

  /** The add filenames to result filenames flag */
  @HopMetadataProperty @Getter @Setter private boolean addFileResult;

  public static final String[] RequiredFilesCode = new String[] {"N", "Y"};

  public static final String NO = "N";

  public static final String YES = "Y";

  @HopMetadataProperty(key = "filename_field")
  private String filenameField;

  /** The fields to import... */
  @HopMetadataProperty(groupKey = "fields", key = "field")
  @Getter
  @Setter
  private List<ParquetFileInputField> fields;

  public ParquetInputEnhancedMeta() {
    fields = new ArrayList<>();
    fileItems = new ArrayList<>();
  }

  @Override
  public void getFields(
      IRowMeta inputRowMeta,
      String name,
      IRowMeta[] info,
      TransformMeta nextTransform,
      IVariables variables,
      IHopMetadataProvider metadataProvider)
      throws HopTransformException {
    // Add the fields to the input
    //
    for (ParquetFileInputField field : fields) {
      try {
        IValueMeta valueMeta = field.createValueMeta();
        valueMeta.setOrigin(name);
        inputRowMeta.addValueMeta(valueMeta);
      } catch (HopException e) {
        throw new HopTransformException(
            "Unable to create value metadata of type '" + field.getTargetType() + "'", e);
      }
    }
  }

  /**
   * Gets filenameField
   *
   * @return value of filenameField
   */
  public String getFilenameField() {
    return filenameField;
  }

  /**
   * Gets fields
   *
   * @return value of fields
   */
  public List<ParquetFileInputField> getFields() {
    return fields;
  }

  /**
   * @param fields The fields to set
   */
  public void setFields(List<ParquetFileInputField> fields) {
    this.fields = fields;
  }

  public static String[] getFilePaths(IVariables variables, List<ParquetFileItem> fileItem) {

    if (fileItem == null || fileItem.isEmpty()) {
      return new String[0];
    }

    String[] fileName = new String[fileItem.size()];
    String[] fileMask = new String[fileItem.size()];
    String[] excludeFileMask = new String[fileItem.size()];
    String[] fileRequired = new String[fileItem.size()];
    boolean[] includeSubDirs = new boolean[fileItem.size()];
    for (int i = 0; i < fileItem.size(); i++) {

      ParquetFileItem item = fileItem.get(i);
      fileName[i] = item.getFileName();
      fileMask[i] = item.getFileMask();
      excludeFileMask[i] = item.getExcludeFileMask();
      fileRequired[i] = item.getFileRequired();
      includeSubDirs[i] = YES.equals(item.getIncludeSubFolders());
    }

    List<FileObject> fileList =
        FileInputList.createFileList(
                variables, fileName, fileMask, excludeFileMask, fileRequired, includeSubDirs)
            .getFiles();
    String[] filePaths = new String[fileList.size()];

    for (int i = 0; i < filePaths.length; ++i) {
      filePaths[i] = ((FileObject) fileList.get(i)).getName().getURI();
    }

    return filePaths;
  }

  //
  //  private boolean[] includeSubFolderBoolean() {
  //    int len = fileName.length;
  //    boolean[] includeSubFolderBoolean = new boolean[len];
  //    for (int i = 0; i < len; i++) {
  //      includeSubFolderBoolean[i] = YES.equalsIgnoreCase(includeSubFolders[i]);
  //    }
  //    return includeSubFolderBoolean;
  //  }

  public String getRequiredFilesDesc(String tt) {
    if (tt == null) {
      return RequiredFilesDesc[0];
    }
    if (tt.equals(RequiredFilesCode[1])) {
      return RequiredFilesDesc[1];
    } else {
      return RequiredFilesDesc[0];
    }
  }
}
