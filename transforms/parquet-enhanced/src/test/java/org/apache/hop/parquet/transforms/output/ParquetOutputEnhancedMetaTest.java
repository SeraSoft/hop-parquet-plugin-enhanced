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

import java.util.List;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.junit.jupiter.api.Test;

class ParquetOutputEnhancedMetaTest {

  @Test
  void defaultConstructorInitializesFieldsCorrectly() {
    ParquetOutputEnhancedMeta meta = new ParquetOutputEnhancedMeta();

    assertEquals("parquet", meta.getFilenameExtension());
    assertEquals("yyyyMMdd-HHmmss", meta.getFilenameDateTimeFormat());
    assertEquals(CompressionCodecName.UNCOMPRESSED, meta.getCompressionCodec());
    assertEquals(ParquetVersion.Version2, meta.getVersion());
    assertEquals("268435456", meta.getRowGroupSize());
    assertEquals("8192", meta.getDataPageSize());
    assertNotNull(meta.getDictionaryPageSize());
    assertNotNull(meta.getFields());
    assertTrue(meta.getFields().isEmpty());
    assertTrue(meta.isFilenameIncludingCopyNr());
    assertFalse(meta.isFilenameIncludingSplitNr());
    assertTrue(meta.isFilenameCreatingParentFolders());
    assertFalse(meta.isAddToResultFilenames());
    assertFalse(meta.isFilenameInField());
  }

  @Test
  void copyConstructorCreatesIndependentCopy() {
    ParquetOutputEnhancedMeta original = new ParquetOutputEnhancedMeta();
    original.setFilenameBase("/tmp/test");
    original.setFilenameExtension("parquet");
    original.setFilenameIncludingDate(true);
    original.setFilenameIncludingTime(true);
    original.setFilenameIncludingDateTime(false);
    original.setFilenameDateTimeFormat("yyyyMMdd");
    original.setFilenameIncludingCopyNr(true);
    original.setFilenameIncludingSplitNr(false);
    original.setFileSplitSize("1000");
    original.setFilenameCreatingParentFolders(true);
    original.setCompressionCodec(CompressionCodecName.SNAPPY);
    original.setVersion(ParquetVersion.Version1);
    original.setRowGroupSize("100000");
    original.setDataPageSize("4096");
    original.setDictionaryPageSize("2048");
    original.setFilenameInField(true);
    original.setFilenameField("myField");
    original.setAddToResultFilenames(true);
    original.getFields().add(new ParquetField("source1", "target1"));

    ParquetOutputEnhancedMeta copy = new ParquetOutputEnhancedMeta(original);

    // Verify all scalar fields are copied
    assertEquals("/tmp/test", copy.getFilenameBase());
    assertEquals("parquet", copy.getFilenameExtension());
    assertTrue(copy.isFilenameIncludingDate());
    assertTrue(copy.isFilenameIncludingTime());
    assertFalse(copy.isFilenameIncludingDateTime());
    assertEquals("yyyyMMdd", copy.getFilenameDateTimeFormat());
    assertTrue(copy.isFilenameIncludingCopyNr());
    assertFalse(copy.isFilenameIncludingSplitNr());
    assertEquals("1000", copy.getFileSplitSize());
    assertTrue(copy.isFilenameCreatingParentFolders());
    assertEquals(CompressionCodecName.SNAPPY, copy.getCompressionCodec());
    assertEquals(ParquetVersion.Version1, copy.getVersion());
    assertEquals("100000", copy.getRowGroupSize());
    assertEquals("4096", copy.getDataPageSize());
    assertEquals("2048", copy.getDictionaryPageSize());
    assertTrue(copy.isFilenameInField());
    assertEquals("myField", copy.getFilenameField());
    assertTrue(copy.isAddToResultFilenames());

    // Modify original scalar fields and verify copy is unchanged
    original.setFilenameBase("/tmp/changed");
    original.setFilenameExtension("pq");
    original.setCompressionCodec(CompressionCodecName.GZIP);
    original.setVersion(ParquetVersion.Version2);
    original.setRowGroupSize("999");

    assertEquals("/tmp/test", copy.getFilenameBase());
    assertEquals("parquet", copy.getFilenameExtension());
    assertEquals(CompressionCodecName.SNAPPY, copy.getCompressionCodec());
    assertEquals(ParquetVersion.Version1, copy.getVersion());
    assertEquals("100000", copy.getRowGroupSize());
  }

  @Test
  void copyConstructorCreatesDeepCopyOfFieldsList() {
    ParquetOutputEnhancedMeta original = new ParquetOutputEnhancedMeta();
    original.getFields().add(new ParquetField("source1", "target1"));
    original.getFields().add(new ParquetField("source2", "target2"));

    ParquetOutputEnhancedMeta copy = new ParquetOutputEnhancedMeta(original);

    // Verify fields content is the same
    List<ParquetField> copyFields = copy.getFields();
    assertEquals(2, copyFields.size());
    assertEquals("source1", copyFields.get(0).getSourceFieldName());
    assertEquals("target1", copyFields.get(0).getTargetFieldName());
    assertEquals("source2", copyFields.get(1).getSourceFieldName());
    assertEquals("target2", copyFields.get(1).getTargetFieldName());

    // Verify the lists are different instances
    assertNotSame(original.getFields(), copy.getFields());

    // Verify adding to original does not affect copy
    original.getFields().add(new ParquetField("source3", "target3"));
    assertEquals(3, original.getFields().size());
    assertEquals(2, copy.getFields().size());

    // Verify modifying a field in original does not affect copy
    original.getFields().get(0).setSourceFieldName("changed");
    assertEquals("source1", copy.getFields().get(0).getSourceFieldName());
  }
}
