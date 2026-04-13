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

import org.apache.hop.core.HopEnvironment;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.row.IValueMeta;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ParquetFieldTest {

  @BeforeAll
  static void setUp() throws HopException {
    HopEnvironment.init();
  }

  @Test
  void defaultConstructorSetsNullDefaults() {
    ParquetField field = new ParquetField();
    assertNull(field.getSourceField());
    assertNull(field.getTargetField());
    assertNull(field.getTargetType());
    assertNull(field.getTargetFormat());
    assertNull(field.getTargetLength());
    assertNull(field.getTargetPrecision());
  }

  @Test
  void parameterizedConstructorSetsAllFields() {
    ParquetField field = new ParquetField("src", "tgt", "String", "yyyy-MM-dd", "50", "2");

    assertEquals("src", field.getSourceField());
    assertEquals("tgt", field.getTargetField());
    assertEquals("String", field.getTargetType());
    assertEquals("yyyy-MM-dd", field.getTargetFormat());
    assertEquals("50", field.getTargetLength());
    assertEquals("2", field.getTargetPrecision());
  }

  @Test
  void copyConstructorCopiesAllFields() {
    ParquetField original = new ParquetField("src", "tgt", "Number", "###.##", "10", "3");
    ParquetField copy = new ParquetField(original);

    assertEquals(original.getSourceField(), copy.getSourceField());
    assertEquals(original.getTargetField(), copy.getTargetField());
    assertEquals(original.getTargetType(), copy.getTargetType());
    assertEquals(original.getTargetFormat(), copy.getTargetFormat());
    assertEquals(original.getTargetLength(), copy.getTargetLength());
    assertEquals(original.getTargetPrecision(), copy.getTargetPrecision());
  }

  @Test
  void copyConstructorCreatesIndependentCopy() {
    ParquetField original = new ParquetField("src", "tgt", "String", null, null, null);
    ParquetField copy = new ParquetField(original);

    copy.setSourceField("modified");
    assertEquals("src", original.getSourceField());
  }

  @ParameterizedTest
  @CsvSource({
    "String,  2",
    "Number,  1",
    "Integer, 5",
    "Date,    3",
    "Timestamp, 9",
    "Boolean, 4",
    "Binary,  8",
    "BigNumber, 6"
  })
  void createValueMetaWithValidTypes(String typeName, int expectedTypeId) throws HopException {
    ParquetField field = new ParquetField("src", "tgt", typeName, null, null, null);
    IValueMeta valueMeta = field.createValueMeta();

    assertEquals("tgt", valueMeta.getName());
    assertEquals(expectedTypeId, valueMeta.getType());
  }

  @Test
  void createValueMetaWithInvalidTypeDefaultsToNone() throws HopException {
    ParquetField field = new ParquetField("src", "tgt", "NonExistentType", null, null, null);
    IValueMeta valueMeta = field.createValueMeta();

    // ValueMetaFactory.getIdForValueMeta returns 0 (NONE) for unknown types
    assertEquals(0, valueMeta.getType());
  }

  @Test
  void createValueMetaWithNullTargetType() throws HopException {
    ParquetField field = new ParquetField("src", "tgt", null, null, null, null);
    IValueMeta valueMeta = field.createValueMeta();

    // null type name resolves to type 0 (NONE)
    assertEquals(0, valueMeta.getType());
  }

  @Test
  void createValueMetaWithEmptyTargetType() throws HopException {
    ParquetField field = new ParquetField("src", "tgt", "", null, null, null);
    IValueMeta valueMeta = field.createValueMeta();

    assertEquals(0, valueMeta.getType());
  }

  @Test
  void createValueMetaWithValidLengthAndPrecision() throws HopException {
    ParquetField field = new ParquetField("src", "tgt", "Number", null, "50", "3");
    IValueMeta valueMeta = field.createValueMeta();

    assertEquals(50, valueMeta.getLength());
    assertEquals(3, valueMeta.getPrecision());
  }

  @Test
  void createValueMetaNullLengthDefaultsToMinusOne() throws HopException {
    ParquetField field = new ParquetField("src", "tgt", "String", null, null, null);
    IValueMeta valueMeta = field.createValueMeta();

    assertEquals(-1, valueMeta.getLength());
    assertEquals(-1, valueMeta.getPrecision());
  }

  @Test
  void createValueMetaEmptyLengthDefaultsToMinusOne() throws HopException {
    ParquetField field = new ParquetField("src", "tgt", "String", null, "", "");
    IValueMeta valueMeta = field.createValueMeta();

    assertEquals(-1, valueMeta.getLength());
    assertEquals(-1, valueMeta.getPrecision());
  }

  @Test
  void createValueMetaNonNumericLengthDefaultsToMinusOne() throws HopException {
    ParquetField field = new ParquetField("src", "tgt", "String", null, "abc", "xyz");
    IValueMeta valueMeta = field.createValueMeta();

    assertEquals(-1, valueMeta.getLength());
    assertEquals(-1, valueMeta.getPrecision());
  }

  @Test
  void createValueMetaSetsConversionMask() throws HopException {
    ParquetField field = new ParquetField("src", "tgt", "Date", "yyyy-MM-dd", null, null);
    IValueMeta valueMeta = field.createValueMeta();

    assertEquals("yyyy-MM-dd", valueMeta.getConversionMask());
  }

  @Test
  void createValueMetaWithNullFormatSetsNullConversionMask() throws HopException {
    ParquetField field = new ParquetField("src", "tgt", "String", null, null, null);
    IValueMeta valueMeta = field.createValueMeta();

    assertNull(valueMeta.getConversionMask());
  }
}
