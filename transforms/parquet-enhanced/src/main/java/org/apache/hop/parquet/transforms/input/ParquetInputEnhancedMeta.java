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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.hop.core.annotations.Transform;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.fileinput.FileInputList;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.value.ValueMetaBoolean;
import org.apache.hop.core.row.value.ValueMetaDate;
import org.apache.hop.core.row.value.ValueMetaInteger;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.core.util.Utils;
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

  /** The field in which the filename is placed */
  @Getter @Setter @HopMetadataProperty private String acceptingField;

  /** The transformName to accept filenames from */
  @Getter @Setter @HopMetadataProperty private String acceptingTransformName;

  /** The transform to accept filenames from */
  @Getter @Setter private TransformMeta acceptingTransform;

  @HopMetadataProperty(key = "fileEntry", groupKey = "fileEntries")
  @Getter
  @Setter
  private List<ParquetFileItem> fileItems;

  /** The add filenames to result filenames flag */
  @HopMetadataProperty @Getter @Setter private boolean addResultFile;

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


    /** The fieldname that holds the name of the file */
    @HopMetadataProperty(key = "filefield",
            injectionKeyDescription = "Extra output: file field name")
    @Getter
    @Setter
    private String fileField;


    /** Additional fields */
    @HopMetadataProperty(
            key = "shortFileFieldName",
            injectionKeyDescription = "Extra output: short file field name")
    @Getter
    @Setter
    private String shortFileFieldName;

    @HopMetadataProperty(
            key = "pathFieldName",
            injectionKeyDescription = "Extra output: path field name")
    @Getter
    @Setter
    private String pathFieldName;

    @HopMetadataProperty(
            key = "hiddenFieldName",
            injectionKeyDescription = "Extra output: hidden flag field name")
    @Getter
    @Setter
    private String hiddenFieldName;

    @HopMetadataProperty(
            key = "lastModificationTimeFieldName",
            injectionKeyDescription = "Extra output: last modification time field name")
    @Getter
    @Setter
    private String lastModificationTimeFieldName;

    @HopMetadataProperty(
            key = "uriNameFieldName",
            injectionKeyDescription = "Extra output: URI field name")
    @Getter
    @Setter
    private String uriNameFieldName;

    @HopMetadataProperty(
            key = "rootUriNameFieldName",
            injectionKeyDescription = "Extra output: root URI field name")
    @Getter
    @Setter
    private String rootUriNameFieldName;

    @HopMetadataProperty(
            key = "extensionFieldName",
            injectionKeyDescription = "Extra output: extension field name")
    @Getter
    @Setter
    private String extensionFieldName;

    @HopMetadataProperty(
            key = "sizeFieldName",
            injectionKeyDescription = "Extra output: file size field name")
    @Getter
    @Setter
    private String sizeFieldName;

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

      if (!Utils.isEmpty(fileField)) {
          IValueMeta v = new ValueMetaString(fileField);
          v.setLength(250);
          v.setPrecision(-1);
          v.setOrigin(name);
          inputRowMeta.addValueMeta(v);
      }

      // Add additional fields
      //
      if (StringUtils.isNotEmpty(getShortFileFieldName())) {
          IValueMeta v = new ValueMetaString(variables.resolve(getShortFileFieldName()));
          v.setLength(100, -1);
          v.setOrigin(name);
          inputRowMeta.addValueMeta(v);
      }
      if (StringUtils.isNotEmpty(getExtensionFieldName())) {
          IValueMeta v = new ValueMetaString(variables.resolve(getExtensionFieldName()));
          v.setLength(100, -1);
          v.setOrigin(name);
          inputRowMeta.addValueMeta(v);
      }
      if (StringUtils.isNotEmpty(getPathFieldName())) {
          IValueMeta v = new ValueMetaString(variables.resolve(getPathFieldName()));
          v.setLength(100, -1);
          v.setOrigin(name);
          inputRowMeta.addValueMeta(v);
      }
      if (StringUtils.isNotEmpty(getSizeFieldName())) {
          IValueMeta v = new ValueMetaInteger(variables.resolve(getSizeFieldName()));
          v.setOrigin(name);
          v.setLength(9);
          inputRowMeta.addValueMeta(v);
      }
      if (StringUtils.isNotEmpty(getHiddenFieldName())) {
          IValueMeta v = new ValueMetaBoolean(variables.resolve(getHiddenFieldName()));
          v.setOrigin(name);
          inputRowMeta.addValueMeta(v);
      }

      if (StringUtils.isNotEmpty(getLastModificationTimeFieldName())) {
          IValueMeta v = new ValueMetaDate(variables.resolve(getLastModificationTimeFieldName()));
          v.setOrigin(name);
          inputRowMeta.addValueMeta(v);
      }
      if (StringUtils.isNotEmpty(getUriNameFieldName())) {
          IValueMeta v = new ValueMetaString(variables.resolve(getUriNameFieldName()));
          v.setLength(100, -1);
          v.setOrigin(name);
          inputRowMeta.addValueMeta(v);
      }

      if (StringUtils.isNotEmpty(getRootUriNameFieldName())) {
          IValueMeta v = new ValueMetaString(variables.resolve(getRootUriNameFieldName()));
          v.setLength(100, -1);
          v.setOrigin(name);
          inputRowMeta.addValueMeta(v);
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

  public String[] getFilePaths(IVariables variables, List<ParquetFileItem> fileItems) {
    String[] filePaths = new String[0];

    if (!(fileItems == null || fileItems.isEmpty())) {

      FileInputList fil = getFileList(variables);
      List<FileObject> fileList = fil.getFiles();

      if (!(fileList == null || fileList.isEmpty())) {
        filePaths = new String[fileList.size()];

        for (int i = 0; i < filePaths.length; ++i) {
          filePaths[i] = ((FileObject) fileList.get(i)).getName().getURI();
        }
      }
    }
    return filePaths;
  }

  public FileInputList getFileList(IVariables variables) {

    if (fileItems == null || fileItems.isEmpty()) {
      return null;
    }

    String[] fileName = new String[fileItems.size()];
    String[] fileMask = new String[fileItems.size()];
    String[] excludeFileMask = new String[fileItems.size()];
    String[] fileRequired = new String[fileItems.size()];
    boolean[] includeSubDirs = new boolean[fileItems.size()];
    for (int i = 0; i < fileItems.size(); i++) {

      ParquetFileItem item = fileItems.get(i);
      fileName[i] = variables.resolve(item.getFileName());
      fileMask[i] = variables.resolve(item.getFileMask());
      excludeFileMask[i] = variables.resolve(item.getExcludeFileMask());
      fileRequired[i] = item.getFileRequired();
      includeSubDirs[i] = YES.equals(item.getIncludeSubFolders());
    }

    return FileInputList.createFileList(
        variables, fileName, fileMask, excludeFileMask, fileRequired, includeSubDirs);
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
