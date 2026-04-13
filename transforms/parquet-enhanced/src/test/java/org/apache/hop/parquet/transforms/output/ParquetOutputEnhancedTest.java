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

package org.apache.hop.parquet.transforms.output;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.hop.pipeline.transform.BaseTransform;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ParquetOutputEnhanced#buildFilename(Object[], Date)}.
 *
 * <p>The buildFilename method is private, so we use reflection to invoke it. The method depends on:
 * meta (flags controlling filename composition), data (split counter, beam context,
 * filenameFieldIndex), and the transform itself (getCopyNr(), resolve()).
 *
 * <p>Because BaseTransform's constructor requires a fully initialized Pipeline (with variable
 * support), we mock ParquetOutputEnhanced directly and set the required fields via reflection to
 * avoid complex Pipeline/TransformMeta initialization.
 */
@ExtendWith(MockitoExtension.class)
class ParquetOutputEnhancedTest {

  private ParquetOutputEnhancedMeta meta;
  private ParquetOutputEnhancedData data;
  private ParquetOutputEnhanced transform;

  private Method buildFilenameMethod;
  private Date executionDate;

  /**
   * Creates a mock of ParquetOutputEnhanced with the given copyNr, and injects meta/data fields via
   * reflection. We use a mock with CALLS_REAL_METHODS so that the private buildFilename() runs real
   * code, while avoiding the BaseTransform constructor entirely.
   */
  private ParquetOutputEnhanced createTransformMock(int copyNr) throws Exception {
    ParquetOutputEnhanced mockTransform = mock(ParquetOutputEnhanced.class, CALLS_REAL_METHODS);

    // Inject meta into the BaseTransform.meta field
    Field metaField = BaseTransform.class.getDeclaredField("meta");
    metaField.setAccessible(true);
    metaField.set(mockTransform, meta);

    // Inject data into the BaseTransform.data field
    Field dataField = BaseTransform.class.getDeclaredField("data");
    dataField.setAccessible(true);
    dataField.set(mockTransform, data);

    // Set the copyNr
    Field copyNrField = BaseTransform.class.getDeclaredField("copyNr");
    copyNrField.setAccessible(true);
    copyNrField.setInt(mockTransform, copyNr);

    // Make resolve() return the input string (no variable substitution)
    // Use lenient() because setUp always creates a mock, but some tests create a second one
    lenient()
        .doAnswer(invocation -> invocation.getArgument(0))
        .when(mockTransform)
        .resolve(anyString());
    // Also handle resolve(null) which anyString() does not match
    lenient().doReturn(null).when(mockTransform).resolve((String) isNull());

    return mockTransform;
  }

  @BeforeEach
  void setUp() throws Exception {
    meta = new ParquetOutputEnhancedMeta();
    data = new ParquetOutputEnhancedData();

    transform = createTransformMock(0);

    // Access the private buildFilename method
    buildFilenameMethod =
        ParquetOutputEnhanced.class.getDeclaredMethod("buildFilename", Object[].class, Date.class);
    buildFilenameMethod.setAccessible(true);

    // Use a fixed date for deterministic tests
    executionDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2025-06-15 14:30:45");
  }

  private String invokeBuildFilename(Object[] row) throws Exception {
    return (String) buildFilenameMethod.invoke(transform, row, executionDate);
  }

  @Test
  void baseFilenameOnly() throws Exception {
    meta.setFilenameBase("/tmp/output");
    meta.setFilenameExtension("parquet");
    meta.setFilenameIncludingCopyNr(false);
    meta.setFilenameIncludingSplitNr(false);
    meta.setCompressionCodec(CompressionCodecName.UNCOMPRESSED);

    String result = invokeBuildFilename(null);

    assertEquals("/tmp/output.parquet", result);
  }

  @Test
  void filenameWithDateFlag() throws Exception {
    meta.setFilenameBase("/tmp/output");
    meta.setFilenameExtension("parquet");
    meta.setFilenameIncludingDate(true);
    meta.setFilenameIncludingCopyNr(false);
    meta.setFilenameIncludingSplitNr(false);
    meta.setCompressionCodec(CompressionCodecName.UNCOMPRESSED);

    String result = invokeBuildFilename(null);

    String expectedDate = new SimpleDateFormat("yyyyMMdd").format(executionDate);
    assertEquals("/tmp/output-" + expectedDate + ".parquet", result);
  }

  @Test
  void filenameWithTimeFlag() throws Exception {
    meta.setFilenameBase("/tmp/output");
    meta.setFilenameExtension("parquet");
    meta.setFilenameIncludingTime(true);
    meta.setFilenameIncludingCopyNr(false);
    meta.setFilenameIncludingSplitNr(false);
    meta.setCompressionCodec(CompressionCodecName.UNCOMPRESSED);

    String result = invokeBuildFilename(null);

    String expectedTime = new SimpleDateFormat("HHmmss").format(executionDate);
    assertEquals("/tmp/output-" + expectedTime + ".parquet", result);
  }

  @Test
  void filenameWithDateTimeFlag() throws Exception {
    meta.setFilenameBase("/tmp/output");
    meta.setFilenameExtension("parquet");
    meta.setFilenameIncludingDateTime(true);
    meta.setFilenameDateTimeFormat("yyyyMMdd-HHmmss");
    meta.setFilenameIncludingCopyNr(false);
    meta.setFilenameIncludingSplitNr(false);
    meta.setCompressionCodec(CompressionCodecName.UNCOMPRESSED);

    String result = invokeBuildFilename(null);

    String expectedDateTime = new SimpleDateFormat("yyyyMMdd-HHmmss").format(executionDate);
    assertEquals("/tmp/output-" + expectedDateTime + ".parquet", result);
  }

  @Test
  void filenameWithCopyNr() throws Exception {
    meta.setFilenameBase("/tmp/output");
    meta.setFilenameExtension("parquet");
    meta.setFilenameIncludingCopyNr(true);
    meta.setFilenameIncludingSplitNr(false);
    meta.setCompressionCodec(CompressionCodecName.UNCOMPRESSED);

    // getCopyNr() returns 0 by default (set in createTransformMock)
    String result = invokeBuildFilename(null);

    assertEquals("/tmp/output-00.parquet", result);
  }

  @Test
  void filenameWithCopyNrNonZero() throws Exception {
    transform = createTransformMock(3);

    meta.setFilenameBase("/tmp/output");
    meta.setFilenameExtension("parquet");
    meta.setFilenameIncludingCopyNr(true);
    meta.setFilenameIncludingSplitNr(false);
    meta.setCompressionCodec(CompressionCodecName.UNCOMPRESSED);

    String result = invokeBuildFilename(null);

    assertEquals("/tmp/output-03.parquet", result);
  }

  @Test
  void filenameWithSplitNr() throws Exception {
    meta.setFilenameBase("/tmp/output");
    meta.setFilenameExtension("parquet");
    meta.setFilenameIncludingCopyNr(false);
    meta.setFilenameIncludingSplitNr(true);
    meta.setCompressionCodec(CompressionCodecName.UNCOMPRESSED);

    data.split = 5;

    String result = invokeBuildFilename(null);

    assertEquals("/tmp/output-0005.parquet", result);
  }

  @Test
  void filenameWithBeamContext() throws Exception {
    meta.setFilenameBase("/tmp/output");
    meta.setFilenameExtension("parquet");
    meta.setFilenameIncludingCopyNr(false);
    meta.setFilenameIncludingSplitNr(false);
    meta.setCompressionCodec(CompressionCodecName.UNCOMPRESSED);

    data.setBeamContext(true);
    data.setBeamBundleNr(42);

    // Mock getLogChannelId
    doReturn("log-channel-123").when(transform).getLogChannelId();

    String result = invokeBuildFilename(null);

    assertEquals("/tmp/output_log-channel-123_42.parquet", result);
  }

  @Test
  void filenameFromFieldInRow() throws Exception {
    meta.setFilenameInField(true);
    meta.setFilenameExtension("parquet");
    meta.setFilenameIncludingCopyNr(false);
    meta.setFilenameIncludingSplitNr(false);
    meta.setCompressionCodec(CompressionCodecName.UNCOMPRESSED);

    data.filenameFieldIndex = 2;

    Object[] row = new Object[] {"val1", "val2", "/data/dynamic_file", "val3"};

    String result = invokeBuildFilename(row);

    assertEquals("/data/dynamic_file.parquet", result);
  }

  @Test
  void filenameFromFieldWithCopyNrAndSplitNr() throws Exception {
    transform = createTransformMock(1);

    meta.setFilenameInField(true);
    meta.setFilenameExtension("parquet");
    meta.setFilenameIncludingCopyNr(true);
    meta.setFilenameIncludingSplitNr(true);
    meta.setCompressionCodec(CompressionCodecName.UNCOMPRESSED);

    data.filenameFieldIndex = 0;
    data.split = 12;

    Object[] row = new Object[] {"/data/output"};

    String result = invokeBuildFilename(row);

    assertEquals("/data/output-01-0012.parquet", result);
  }

  @Test
  void filenameWithSnappyCompression() throws Exception {
    meta.setFilenameBase("/tmp/output");
    meta.setFilenameExtension("parquet");
    meta.setFilenameIncludingCopyNr(false);
    meta.setFilenameIncludingSplitNr(false);
    meta.setCompressionCodec(CompressionCodecName.SNAPPY);

    String result = invokeBuildFilename(null);

    assertEquals("/tmp/output.parquet.snappy", result);
  }

  @Test
  void filenameWithGzipCompression() throws Exception {
    meta.setFilenameBase("/tmp/output");
    meta.setFilenameExtension("parquet");
    meta.setFilenameIncludingCopyNr(false);
    meta.setFilenameIncludingSplitNr(false);
    meta.setCompressionCodec(CompressionCodecName.GZIP);

    String result = invokeBuildFilename(null);

    assertEquals("/tmp/output.parquet.gz", result);
  }

  @Test
  void filenameWithNullExtensionDefaultsToParquet() throws Exception {
    meta.setFilenameBase("/tmp/output");
    meta.setFilenameExtension(null);
    meta.setFilenameIncludingCopyNr(false);
    meta.setFilenameIncludingSplitNr(false);
    meta.setCompressionCodec(CompressionCodecName.UNCOMPRESSED);

    String result = invokeBuildFilename(null);

    assertEquals("/tmp/output.parquet", result);
  }

  @Test
  void filenameWithAllFlagsCombined() throws Exception {
    meta.setFilenameBase("/tmp/output");
    meta.setFilenameExtension("pq");
    meta.setFilenameIncludingDate(true);
    meta.setFilenameIncludingTime(true);
    meta.setFilenameIncludingDateTime(true);
    meta.setFilenameDateTimeFormat("yyyyMMdd-HHmmss");
    meta.setFilenameIncludingCopyNr(true);
    meta.setFilenameIncludingSplitNr(true);
    meta.setCompressionCodec(CompressionCodecName.SNAPPY);

    data.split = 7;

    String result = invokeBuildFilename(null);

    String expectedDate = new SimpleDateFormat("yyyyMMdd").format(executionDate);
    String expectedTime = new SimpleDateFormat("HHmmss").format(executionDate);
    String expectedDateTime = new SimpleDateFormat("yyyyMMdd-HHmmss").format(executionDate);

    String expected =
        "/tmp/output-"
            + expectedDate
            + "-"
            + expectedTime
            + "-"
            + expectedDateTime
            + "-"
            + new DecimalFormat("00").format(0)
            + "-"
            + new DecimalFormat("0000").format(7)
            + ".pq.snappy";

    assertEquals(expected, result);
  }

  @Test
  void filenameFromFieldWithCompression() throws Exception {
    meta.setFilenameInField(true);
    meta.setFilenameExtension("parquet");
    meta.setFilenameIncludingCopyNr(false);
    meta.setFilenameIncludingSplitNr(false);
    meta.setCompressionCodec(CompressionCodecName.GZIP);

    data.filenameFieldIndex = 0;

    Object[] row = new Object[] {"/data/compressed"};

    String result = invokeBuildFilename(row);

    assertEquals("/data/compressed.parquet.gz", result);
  }

  @Test
  void filenameInFieldPathDoesNotIncludeDateOrTime() throws Exception {
    // When filenameInField is true, date/time/datetime flags are ignored
    meta.setFilenameInField(true);
    meta.setFilenameExtension("parquet");
    meta.setFilenameIncludingDate(true);
    meta.setFilenameIncludingTime(true);
    meta.setFilenameIncludingDateTime(true);
    meta.setFilenameIncludingCopyNr(false);
    meta.setFilenameIncludingSplitNr(false);
    meta.setCompressionCodec(CompressionCodecName.UNCOMPRESSED);

    data.filenameFieldIndex = 0;

    Object[] row = new Object[] {"/data/field_file"};

    String result = invokeBuildFilename(row);

    // Date, time, and datetime flags should NOT affect the field-based path
    assertEquals("/data/field_file.parquet", result);
  }
}
