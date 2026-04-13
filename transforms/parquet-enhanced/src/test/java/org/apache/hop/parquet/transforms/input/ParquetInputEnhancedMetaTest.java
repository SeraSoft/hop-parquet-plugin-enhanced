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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.HopEnvironment;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.variables.Variables;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ParquetInputEnhancedMeta#getFields(IRowMeta, String, IRowMeta[],
 * org.apache.hop.pipeline.transform.TransformMeta, IVariables,
 * org.apache.hop.metadata.api.IHopMetadataProvider)}.
 *
 * <p>Tests cover: field list population with various types, optional file metadata fields, and
 * variable resolution. The metadataFilename path (which reads an actual parquet file) is not tested
 * here as it requires file I/O.
 */
class ParquetInputEnhancedMetaTest {

  private ParquetInputEnhancedMeta meta;
  private IVariables variables;

  @BeforeAll
  static void initHop() throws HopException {
    HopEnvironment.init();
  }

  @BeforeEach
  void setUp() {
    meta = new ParquetInputEnhancedMeta();
    variables = new Variables();
  }

  private IRowMeta callGetFields() throws HopTransformException {
    IRowMeta rowMeta = new RowMeta();
    meta.getFields(rowMeta, "testOrigin", null, null, variables, null);
    return rowMeta;
  }

  // --- Field list population tests ---

  @Test
  void emptyFieldsListProducesNoOutputFields() throws HopTransformException {
    // fields is empty, metadataFilename is null => nothing added
    IRowMeta rowMeta = callGetFields();
    assertEquals(0, rowMeta.size());
  }

  @Test
  void singleStringField() throws HopTransformException {
    meta.getFields().add(new ParquetField("src", "target_str", "String", null, null, null));

    IRowMeta rowMeta = callGetFields();

    assertEquals(1, rowMeta.size());
    IValueMeta vm = rowMeta.getValueMeta(0);
    assertEquals("target_str", vm.getName());
    assertEquals(IValueMeta.TYPE_STRING, vm.getType());
    assertEquals("testOrigin", vm.getOrigin());
  }

  @Test
  void multipleFieldsWithVariousTypes() throws HopTransformException {
    List<ParquetField> fields = new ArrayList<>();
    fields.add(new ParquetField("s1", "col_string", "String", null, "100", null));
    fields.add(new ParquetField("s2", "col_integer", "Integer", null, null, null));
    fields.add(new ParquetField("s3", "col_number", "Number", "###.##", "10", "2"));
    fields.add(new ParquetField("s4", "col_date", "Date", "yyyy-MM-dd", null, null));
    fields.add(new ParquetField("s5", "col_boolean", "Boolean", null, null, null));
    fields.add(new ParquetField("s6", "col_binary", "Binary", null, null, null));
    fields.add(new ParquetField("s7", "col_bignumber", "BigNumber", null, null, null));
    fields.add(new ParquetField("s8", "col_timestamp", "Timestamp", null, null, null));
    meta.setFields(fields);

    IRowMeta rowMeta = callGetFields();

    assertEquals(8, rowMeta.size());

    assertEquals("col_string", rowMeta.getValueMeta(0).getName());
    assertEquals(IValueMeta.TYPE_STRING, rowMeta.getValueMeta(0).getType());
    assertEquals(100, rowMeta.getValueMeta(0).getLength());

    assertEquals("col_integer", rowMeta.getValueMeta(1).getName());
    assertEquals(IValueMeta.TYPE_INTEGER, rowMeta.getValueMeta(1).getType());

    assertEquals("col_number", rowMeta.getValueMeta(2).getName());
    assertEquals(IValueMeta.TYPE_NUMBER, rowMeta.getValueMeta(2).getType());
    assertEquals("###.##", rowMeta.getValueMeta(2).getConversionMask());
    assertEquals(10, rowMeta.getValueMeta(2).getLength());
    assertEquals(2, rowMeta.getValueMeta(2).getPrecision());

    assertEquals("col_date", rowMeta.getValueMeta(3).getName());
    assertEquals(IValueMeta.TYPE_DATE, rowMeta.getValueMeta(3).getType());

    assertEquals("col_boolean", rowMeta.getValueMeta(4).getName());
    assertEquals(IValueMeta.TYPE_BOOLEAN, rowMeta.getValueMeta(4).getType());

    assertEquals("col_binary", rowMeta.getValueMeta(5).getName());
    assertEquals(IValueMeta.TYPE_BINARY, rowMeta.getValueMeta(5).getType());

    assertEquals("col_bignumber", rowMeta.getValueMeta(6).getName());
    assertEquals(IValueMeta.TYPE_BIGNUMBER, rowMeta.getValueMeta(6).getType());

    assertEquals("col_timestamp", rowMeta.getValueMeta(7).getName());
    assertEquals(IValueMeta.TYPE_TIMESTAMP, rowMeta.getValueMeta(7).getType());
  }

  @Test
  void allFieldsHaveCorrectOrigin() throws HopTransformException {
    meta.getFields().add(new ParquetField("s1", "f1", "String", null, null, null));
    meta.getFields().add(new ParquetField("s2", "f2", "Integer", null, null, null));

    IRowMeta rowMeta = callGetFields();

    for (int i = 0; i < rowMeta.size(); i++) {
      assertEquals("testOrigin", rowMeta.getValueMeta(i).getOrigin());
    }
  }

  // --- File field (fileField) tests ---

  @Test
  void fileFieldAddedWhenSet() throws HopTransformException {
    meta.getFields().add(new ParquetField("s1", "f1", "String", null, null, null));
    meta.setFileField("filename_col");

    IRowMeta rowMeta = callGetFields();

    assertEquals(2, rowMeta.size());
    IValueMeta fileVm = rowMeta.getValueMeta(1);
    assertEquals("filename_col", fileVm.getName());
    assertEquals(IValueMeta.TYPE_STRING, fileVm.getType());
    assertEquals(250, fileVm.getLength());
    assertEquals("testOrigin", fileVm.getOrigin());
  }

  @Test
  void fileFieldNotAddedWhenNull() throws HopTransformException {
    meta.getFields().add(new ParquetField("s1", "f1", "String", null, null, null));
    meta.setFileField(null);

    IRowMeta rowMeta = callGetFields();
    assertEquals(1, rowMeta.size());
  }

  @Test
  void fileFieldNotAddedWhenEmpty() throws HopTransformException {
    meta.getFields().add(new ParquetField("s1", "f1", "String", null, null, null));
    meta.setFileField("");

    IRowMeta rowMeta = callGetFields();
    assertEquals(1, rowMeta.size());
  }

  // --- Additional metadata fields tests ---

  @Test
  void shortFileFieldAddedWhenSet() throws HopTransformException {
    meta.setShortFileFieldName("short_file");

    IRowMeta rowMeta = callGetFields();

    assertEquals(1, rowMeta.size());
    IValueMeta vm = rowMeta.getValueMeta(0);
    assertEquals("short_file", vm.getName());
    assertEquals(IValueMeta.TYPE_STRING, vm.getType());
    assertEquals(100, vm.getLength());
    assertEquals("testOrigin", vm.getOrigin());
  }

  @Test
  void extensionFieldAddedWhenSet() throws HopTransformException {
    meta.setExtensionFieldName("ext_field");

    IRowMeta rowMeta = callGetFields();

    assertEquals(1, rowMeta.size());
    IValueMeta vm = rowMeta.getValueMeta(0);
    assertEquals("ext_field", vm.getName());
    assertEquals(IValueMeta.TYPE_STRING, vm.getType());
    assertEquals(100, vm.getLength());
  }

  @Test
  void pathFieldAddedWhenSet() throws HopTransformException {
    meta.setPathFieldName("path_field");

    IRowMeta rowMeta = callGetFields();

    assertEquals(1, rowMeta.size());
    IValueMeta vm = rowMeta.getValueMeta(0);
    assertEquals("path_field", vm.getName());
    assertEquals(IValueMeta.TYPE_STRING, vm.getType());
    assertEquals(100, vm.getLength());
  }

  @Test
  void sizeFieldAddedWhenSet() throws HopTransformException {
    meta.setSizeFieldName("size_field");

    IRowMeta rowMeta = callGetFields();

    assertEquals(1, rowMeta.size());
    IValueMeta vm = rowMeta.getValueMeta(0);
    assertEquals("size_field", vm.getName());
    assertEquals(IValueMeta.TYPE_INTEGER, vm.getType());
    assertEquals(9, vm.getLength());
  }

  @Test
  void hiddenFieldAddedWhenSet() throws HopTransformException {
    meta.setHiddenFieldName("hidden_field");

    IRowMeta rowMeta = callGetFields();

    assertEquals(1, rowMeta.size());
    IValueMeta vm = rowMeta.getValueMeta(0);
    assertEquals("hidden_field", vm.getName());
    assertEquals(IValueMeta.TYPE_BOOLEAN, vm.getType());
  }

  @Test
  void lastModificationTimeFieldAddedWhenSet() throws HopTransformException {
    meta.setLastModificationTimeFieldName("last_mod");

    IRowMeta rowMeta = callGetFields();

    assertEquals(1, rowMeta.size());
    IValueMeta vm = rowMeta.getValueMeta(0);
    assertEquals("last_mod", vm.getName());
    assertEquals(IValueMeta.TYPE_DATE, vm.getType());
  }

  @Test
  void uriFieldAddedWhenSet() throws HopTransformException {
    meta.setUriNameFieldName("uri_field");

    IRowMeta rowMeta = callGetFields();

    assertEquals(1, rowMeta.size());
    IValueMeta vm = rowMeta.getValueMeta(0);
    assertEquals("uri_field", vm.getName());
    assertEquals(IValueMeta.TYPE_STRING, vm.getType());
    assertEquals(100, vm.getLength());
  }

  @Test
  void rootUriFieldAddedWhenSet() throws HopTransformException {
    meta.setRootUriNameFieldName("root_uri_field");

    IRowMeta rowMeta = callGetFields();

    assertEquals(1, rowMeta.size());
    IValueMeta vm = rowMeta.getValueMeta(0);
    assertEquals("root_uri_field", vm.getName());
    assertEquals(IValueMeta.TYPE_STRING, vm.getType());
    assertEquals(100, vm.getLength());
  }

  @Test
  void noMetadataFieldsAddedWhenAllNamesAreNull() throws HopTransformException {
    // All additional field names are null by default
    IRowMeta rowMeta = callGetFields();
    assertEquals(0, rowMeta.size());
  }

  @Test
  void allMetadataFieldsAddedTogether() throws HopTransformException {
    meta.getFields().add(new ParquetField("s1", "f1", "String", null, null, null));
    meta.setFileField("file_col");
    meta.setShortFileFieldName("short");
    meta.setExtensionFieldName("ext");
    meta.setPathFieldName("path");
    meta.setSizeFieldName("size");
    meta.setHiddenFieldName("hidden");
    meta.setLastModificationTimeFieldName("lastmod");
    meta.setUriNameFieldName("uri");
    meta.setRootUriNameFieldName("rooturi");

    IRowMeta rowMeta = callGetFields();

    // 1 data field + fileField + 8 metadata fields = 10
    assertEquals(10, rowMeta.size());

    assertEquals("f1", rowMeta.getValueMeta(0).getName());
    assertEquals("file_col", rowMeta.getValueMeta(1).getName());
    assertEquals("short", rowMeta.getValueMeta(2).getName());
    assertEquals("ext", rowMeta.getValueMeta(3).getName());
    assertEquals("path", rowMeta.getValueMeta(4).getName());
    assertEquals("size", rowMeta.getValueMeta(5).getName());
    assertEquals("hidden", rowMeta.getValueMeta(6).getName());
    assertEquals("lastmod", rowMeta.getValueMeta(7).getName());
    assertEquals("uri", rowMeta.getValueMeta(8).getName());
    assertEquals("rooturi", rowMeta.getValueMeta(9).getName());
  }

  // --- Variable resolution tests ---

  @Test
  void metadataFieldNamesResolveVariables() throws HopTransformException {
    variables.setVariable("SHORT_NAME", "resolved_short");
    variables.setVariable("EXT_NAME", "resolved_ext");

    meta.setShortFileFieldName("${SHORT_NAME}");
    meta.setExtensionFieldName("${EXT_NAME}");

    IRowMeta rowMeta = callGetFields();

    assertEquals(2, rowMeta.size());
    assertEquals("resolved_short", rowMeta.getValueMeta(0).getName());
    assertEquals("resolved_ext", rowMeta.getValueMeta(1).getName());
  }

  @Test
  void fieldWithLengthAndPrecision() throws HopTransformException {
    meta.getFields().add(new ParquetField("src", "num_field", "Number", "###.##", "15", "4"));

    IRowMeta rowMeta = callGetFields();

    assertEquals(1, rowMeta.size());
    IValueMeta vm = rowMeta.getValueMeta(0);
    assertEquals(15, vm.getLength());
    assertEquals(4, vm.getPrecision());
    assertEquals("###.##", vm.getConversionMask());
  }
}
