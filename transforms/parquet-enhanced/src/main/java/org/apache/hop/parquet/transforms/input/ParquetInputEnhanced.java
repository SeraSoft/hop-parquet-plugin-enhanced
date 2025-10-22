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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.hop.core.Result;
import org.apache.hop.core.ResultFile;
import org.apache.hop.core.RowMetaAndData;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopFileException;
import org.apache.hop.core.fileinput.FileInputList;
import org.apache.hop.core.row.RowDataUtil;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.vfs.HopVfs;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransform;
import org.apache.hop.pipeline.transform.TransformMeta;

public class ParquetInputEnhanced
    extends BaseTransform<ParquetInputEnhancedMeta, ParquetInputEnhancedData> {

  private static final Class<?> PKG = ParquetInputEnhancedMeta.class; // For Translator

  public ParquetInputEnhanced(
      TransformMeta transformMeta,
      ParquetInputEnhancedMeta meta,
      ParquetInputEnhancedData data,
      int copyNr,
      PipelineMeta pipelineMeta,
      Pipeline pipeline) {
    super(transformMeta, meta, data, copyNr, pipelineMeta, pipeline);
  }

  @Override
  public boolean init() {
    if (super.init()) {

      Result previousResult = getPipeline().getPreviousResult();
      Map<String, ResultFile> resultFiles =
          (previousResult != null) ? previousResult.getResultFiles() : null;

      data.files = meta.getFileList(this);
      if (data.files.nrOfFiles() == 0
          && data.files.nrOfMissingFiles() > 0
          && !meta.isAcceptingFilenames()) {

        logError(BaseMessages.getString(PKG, "ParquetInput.Error.NoFileSpecified"));
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean processRow() throws HopException {

    if (first) {
      first = false;

      data.outputRowMeta = new RowMeta(); // start from scratch!
      meta.getFields(data.outputRowMeta, getTransformName(), null, null, this, metadataProvider);

      if (meta.isAcceptingFilenames()) {
        // Read the files from the specified input stream...
        data.files.getFiles().clear();

        int idx = -1;
        data.rowSet = findInputRowSet(meta.getAcceptingTransformName());

        Object[] fileRow = getRowFrom(data.rowSet);
        while (fileRow != null) {
          if (idx < 0) {
            idx = data.rowSet.getRowMeta().indexOfValue(meta.getAcceptingField());
            if (idx < 0) {
              logError(
                  BaseMessages.getString(
                      PKG,
                      "ParquetInput.Error.FilenameFieldNotFound",
                      "" + meta.getAcceptingField()));

              setErrors(1);
              stopAll();
              return false;
            }
          }
          String fileValue = data.rowSet.getRowMeta().getString(fileRow, idx);
          try {
            data.files.addFile(HopVfs.getFileObject(fileValue, variables));
          } catch (HopFileException e) {
            throw new HopException(
                BaseMessages.getString(
                    PKG, "ParquetInput.Exception.CanNotCreateFileObject", fileValue),
                e);
          }

          // Grab another row
          fileRow = getRowFrom(data.rowSet);
        }
      }

      handleMissingFiles();
    }

    // See if we're not done processing...
    // We are done processing if the filenr >= number of files.
    if (data.filenr >= data.files.nrOfFiles()) {
      if (isDetailed()) {
        logDetailed(BaseMessages.getString(PKG, "ParquetInput.Log.NoMoreFiles", "" + data.filenr));
      }

      setOutputDone(); // signal end to receiver(s)
      return false; // end of data or error.
    }

    //      if (meta.getRowLimit() > 0 && getLinesInput() >= meta.getRowLimit()) {
    //          // The close of the openFile is in dispose()
    //          if (isDetailed()) {
    //              logDetailed(
    //                      BaseMessages.getString(PKG, "ExcelInput.Log.RowLimitReached", "" +
    // meta.getRowLimit()));
    //          }
    //
    //          setOutputDone(); // signal end to receiver(s)
    //          return false; // end of data or error.
    //      }

    getRowsFromParquetFile();

    //    if (r != null) {
    //      incrementLinesInput();
    //
    //      // Send out the good news: we found rows of data!
    //      putRow(data.outputRowMeta, r);
    //
    //      return true;
    //    } else {
    //      // This row is ignored / eaten
    //      // We continue though.
    //      return true;
    //    }

    return true;
  }

  private void getRowsFromParquetFile() throws HopException {
    // This procedure outputs a single Parquet file data row on the destination
    // rowsets...

    try {
      data.file = data.files.getFile(data.filenr);
      data.filename = HopVfs.getFilename(data.file);

      // Add additional fields?
      if (StringUtils.isNotEmpty(meta.getShortFileFieldName())) {
        data.shortFilename = data.file.getName().getBaseName();
      }
      if (StringUtils.isNotEmpty(meta.getPathFieldName())) {
        data.path = HopVfs.getFilename(data.file.getParent());
      }
      if (StringUtils.isNotEmpty(meta.getHiddenFieldName())) {
        data.hidden = data.file.isHidden();
      }
      if (StringUtils.isNotEmpty(meta.getExtensionFieldName())) {
        data.extension = data.file.getName().getExtension();
      }
      if (StringUtils.isNotEmpty(meta.getLastModificationTimeFieldName())) {
        data.lastModificationDateTime = new Date(data.file.getContent().getLastModifiedTime());
      }
      if (StringUtils.isNotEmpty(meta.getUriNameFieldName())) {
        data.uriName = data.file.getName().getURI();
      }
      if (StringUtils.isNotEmpty(meta.getRootUriNameFieldName())) {
        data.rootUriName = data.file.getName().getRootURI();
      }
      if (StringUtils.isNotEmpty(meta.getSizeFieldName())) {
        data.size = data.file.getContent().getSize();
      }

      if (meta.isAddResultFile()) {
        ResultFile resultFile =
            new ResultFile(
                ResultFile.FILE_TYPE_GENERAL, data.file, getPipelineMeta().getName(), toString());
        resultFile.setComment(BaseMessages.getString(PKG, "ExcelInput.Log.FileReadByTransform"));
        addResultFile(resultFile);
      }

      if (isDetailed()) {
        logDetailed(
            BaseMessages.getString(
                PKG, "ExcelInput.Log.OpeningFile", "" + data.filenr + " : " + data.filename));
      }

      FileObject fileObject = HopVfs.getFileObject(data.filename);
      data.inputStream = HopVfs.getInputStream(fileObject);

      // Reads the whole file into memory...
      //
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream((int) data.size);
      IOUtils.copy(data.inputStream, outputStream);
      ParquetStream inputFile = new ParquetStream(outputStream.toByteArray(), data.filename);

      ParquetReadSupport readSupport = new ParquetReadSupport(meta.getFields());
      data.reader = new ParquetReaderBuilder<>(readSupport, inputFile).build();

      RowMetaAndData r = data.reader.read();
      while (r != null && !isStopped()) {

        Object[] row = r.getData();
        int rowIndex = r.size();

        // Do we need to include the filename?
        if (StringUtils.isNotEmpty(meta.getFileField())) {
          row[rowIndex] = data.filename;
          rowIndex++;
        }

        // Do we need to include the rownumber?
        //          if (StringUtils.isNotEmpty(meta.getRowNumberField())) {
        //              row[rowIndex] = getLinesWritten() + 1;
        //              rowIndex++;
        //          }

        // Possibly add short filename...
        if (StringUtils.isNotEmpty(meta.getShortFileFieldName())) {
          row[rowIndex] = data.shortFilename;
          rowIndex++;
        }
        // Add Extension
        if (StringUtils.isNotEmpty(meta.getExtensionFieldName())) {
          row[rowIndex] = data.extension;
          rowIndex++;
        }
        // add path
        if (StringUtils.isNotEmpty(meta.getPathFieldName())) {
          row[rowIndex] = data.path;
          rowIndex++;
        }
        // Add Size
        if (StringUtils.isNotEmpty(meta.getSizeFieldName())) {
          row[rowIndex] = data.size;
          rowIndex++;
        }
        // add Hidden
        if (StringUtils.isNotEmpty(meta.getHiddenFieldName())) {
          row[rowIndex] = data.hidden;
          rowIndex++;
        }
        // Add modification date
        if (StringUtils.isNotEmpty(meta.getLastModificationTimeFieldName())) {
          row[rowIndex] = data.lastModificationDateTime;
          rowIndex++;
        }
        // Add Uri
        if (StringUtils.isNotEmpty(meta.getUriNameFieldName())) {
          row[rowIndex] = data.uriName;
          rowIndex++;
        }
        // Add RootUri
        if (StringUtils.isNotEmpty(meta.getRootUriNameFieldName())) {
          row[rowIndex] = data.rootUriName;
        }

        putRow(data.outputRowMeta, row);
        r = data.reader.read();
      }
      data.filenr++;

    } catch (Exception e) {
      throw new HopException("Error read file " + data.filename, e);
    } finally {
      closeFile();
    }
  }

  public void closeFile() {
    if (data.reader != null && data.inputStream != null) {
      try {
        data.reader.close();
        data.inputStream.close();
      } catch (IOException e) {
        logError("Unable to properly close parquet reader!");
      }
    }
  }

  private void handleMissingFiles() throws HopException {
    List<FileObject> nonExistantFiles = data.files.getNonExistentFiles();

    if (!nonExistantFiles.isEmpty()) {
      String message = FileInputList.getRequiredFilesDescription(nonExistantFiles);
      if (isBasic()) {
        logBasic(
            BaseMessages.getString(PKG, "ExcelInput.Log.RequiredFilesTitle"),
            BaseMessages.getString(PKG, "ExcelInput.Warning.MissingFiles", message));
      }

      //      if (meta.isErrorIgnored()) {
      //        for (FileObject fileObject : nonExistantFiles) {
      //          data.errorHandler.handleNonExistantFile(fileObject);
      //        }
      //      } else {
      //        throw new HopException(
      //            BaseMessages.getString(PKG, "ExcelInput.Exception.MissingRequiredFiles",
      // message));
      //      }
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    closeFile();
  }
}
