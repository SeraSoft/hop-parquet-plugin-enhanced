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

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import org.apache.hop.core.RowMetaAndData;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.junit.jupiter.api.Test;

class ParquetValueConverterTest {

  /**
   * Helper to create a ParquetValueConverter with a mocked RowMetaAndData. The returned
   * RowMetaAndData's getData() array can be inspected after calling add* methods to verify the
   * converted value.
   */
  private static class ConverterFixture {
    final RowMetaAndData group;
    final ParquetValueConverter converter;
    final Object[] data;

    ConverterFixture(int valueMetaType, LogicalTypeAnnotation logicalType) {
      IValueMeta valueMeta = mock(IValueMeta.class);
      when(valueMeta.getType()).thenReturn(valueMetaType);
      when(valueMeta.getTypeDesc()).thenReturn("MockType");

      IRowMeta rowMeta = mock(IRowMeta.class);
      when(rowMeta.getValueMeta(0)).thenReturn(valueMeta);
      when(rowMeta.size()).thenReturn(1);

      data = new Object[1];
      group = new RowMetaAndData(rowMeta, data);
      converter = new ParquetValueConverter(group, 0, logicalType);
    }

    Object result() {
      return data[0];
    }
  }

  // =========================================================================
  // addBoolean tests
  // =========================================================================

  @Test
  void addBoolean_typeBoolean_storesTrue() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_BOOLEAN, null);
    f.converter.addBoolean(true);
    assertEquals(Boolean.TRUE, f.result());
  }

  @Test
  void addBoolean_typeBoolean_storesFalse() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_BOOLEAN, null);
    f.converter.addBoolean(false);
    assertEquals(Boolean.FALSE, f.result());
  }

  @Test
  void addBoolean_typeString_storesStringTrue() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_STRING, null);
    f.converter.addBoolean(true);
    assertEquals("true", f.result());
  }

  @Test
  void addBoolean_typeString_storesStringFalse() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_STRING, null);
    f.converter.addBoolean(false);
    assertEquals("false", f.result());
  }

  @Test
  void addBoolean_typeInteger_stores1ForTrue() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_INTEGER, null);
    f.converter.addBoolean(true);
    assertEquals(1L, f.result());
  }

  @Test
  void addBoolean_typeInteger_stores0ForFalse() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_INTEGER, null);
    f.converter.addBoolean(false);
    assertEquals(0L, f.result());
  }

  @Test
  void addBoolean_unsupportedType_throwsException() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_NUMBER, null);
    assertThrows(RuntimeException.class, () -> f.converter.addBoolean(true));
  }

  // =========================================================================
  // addDouble tests
  // =========================================================================

  @Test
  void addDouble_typeNumber_storesDouble() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_NUMBER, null);
    f.converter.addDouble(3.14);
    assertEquals(3.14, f.result());
  }

  @Test
  void addDouble_typeString_storesString() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_STRING, null);
    f.converter.addDouble(2.718);
    assertEquals("2.718", f.result());
  }

  @Test
  void addDouble_typeBigNumber_storesBigDecimal() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_BIGNUMBER, null);
    f.converter.addDouble(1.5);
    assertEquals(BigDecimal.valueOf(1.5), f.result());
  }

  @Test
  void addDouble_unsupportedType_throwsException() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_BOOLEAN, null);
    assertThrows(RuntimeException.class, () -> f.converter.addDouble(1.0));
  }

  // =========================================================================
  // addLong tests
  // =========================================================================

  @Test
  void addLong_typeInteger_storesLong() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_INTEGER, null);
    f.converter.addLong(42L);
    assertEquals(42L, f.result());
  }

  @Test
  void addLong_typeString_storesString() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_STRING, null);
    f.converter.addLong(99L);
    assertEquals("99", f.result());
  }

  @Test
  void addLong_typeBigNumber_noLogicalType_storesBigDecimal() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_BIGNUMBER, null);
    f.converter.addLong(12345L);
    assertEquals(new BigDecimal(12345), f.result());
  }

  @Test
  void addLong_typeBigNumber_withDecimalLogicalType_scalesCorrectly() {
    LogicalTypeAnnotation.DecimalLogicalTypeAnnotation decimalAnnotation =
        LogicalTypeAnnotation.decimalType(2, 10);
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_BIGNUMBER, decimalAnnotation);
    f.converter.addLong(12345L);
    // 12345 with scale 2 -> 123.45
    BigDecimal result = (BigDecimal) f.result();
    assertEquals(
        0, new BigDecimal("123.45").compareTo(result), "Expected 123.45 but got " + result);
  }

  @Test
  void addLong_typeBigNumber_decimalPrecisionPreserved() {
    // This tests the fix for bug #2: precision should not be lost through double conversion
    LogicalTypeAnnotation.DecimalLogicalTypeAnnotation decimalAnnotation =
        LogicalTypeAnnotation.decimalType(4, 18);
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_BIGNUMBER, decimalAnnotation);
    // Use a value that would lose precision in double: 999999999999999999L
    f.converter.addLong(999999999999999999L);
    BigDecimal result = (BigDecimal) f.result();
    BigDecimal expected = new BigDecimal("99999999999999.9999");
    assertEquals(
        0,
        expected.compareTo(result),
        "Expected " + expected + " but got " + result + " (precision loss detected)");
  }

  @Test
  void addLong_unsupportedType_throwsException() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_BINARY, null);
    assertThrows(RuntimeException.class, () -> f.converter.addLong(1L));
  }

  // =========================================================================
  // addBinary tests
  // =========================================================================

  @Test
  void addBinary_typeString_storesUtf8String() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_STRING, null);
    f.converter.addBinary(Binary.fromString("hello"));
    assertEquals("hello", f.result());
  }

  @Test
  void addBinary_typeBinary_storesBytes() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_BINARY, null);
    byte[] bytes = {1, 2, 3};
    f.converter.addBinary(Binary.fromConstantByteArray(bytes));
    assertArrayEquals(bytes, (byte[]) f.result());
  }

  @Test
  void addBinary_typeBigNumber_fromString() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_BIGNUMBER, null);
    f.converter.addBinary(Binary.fromString("123.456"));
    assertEquals(new BigDecimal("123.456"), f.result());
  }

  @Test
  void addBinary_typeTimestamp_int96_nanosPreserved() {
    // This tests the fix for bug #1: nanoseconds extracted from the original ns value
    // Build an INT96 binary: 8 bytes ns-in-day (LE) + 4 bytes Julian day (LE)
    // Julian day 2440588 = 1970-01-01
    // nsDay = 500_000_123 ns = 0.500000123 seconds
    long nsDay = 500_000_123L;
    int julianDay = 2440588;

    ByteBuffer bb = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
    bb.putLong(nsDay);
    bb.putInt(julianDay);
    Binary int96 = Binary.fromConstantByteArray(bb.array());

    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_TIMESTAMP, null);
    f.converter.addBinary(int96);

    Timestamp ts = (Timestamp) f.result();
    assertNotNull(ts);
    // The sub-second nanoseconds should be 500_000_123
    assertEquals(
        500_000_123,
        ts.getNanos(),
        "Nanoseconds should be extracted from the original nanosecond value, not from milliseconds");
  }

  @Test
  void addBinary_typeTimestamp_int96_epochIsCorrect() {
    // 2440588 Julian day = 1970-01-01 epoch, 0 ns
    long nsDay = 0L;
    int julianDay = 2440588;
    ByteBuffer bb = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
    bb.putLong(nsDay);
    bb.putInt(julianDay);
    Binary int96 = Binary.fromConstantByteArray(bb.array());

    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_TIMESTAMP, null);
    f.converter.addBinary(int96);

    Timestamp ts = (Timestamp) f.result();
    assertEquals(0L, ts.getTime(), "Epoch timestamp should be 0");
    assertEquals(0, ts.getNanos(), "Epoch nanos should be 0");
  }

  @Test
  void addBinary_unsupportedType_throwsException() {
    ConverterFixture f = new ConverterFixture(IValueMeta.TYPE_INTEGER, null);
    assertThrows(RuntimeException.class, () -> f.converter.addBinary(Binary.fromString("test")));
  }

  // =========================================================================
  // binaryToDecimal tests (static method)
  // =========================================================================

  @Test
  void binaryToDecimal_smallPrecision_correctResult() {
    // Encode the value 12345 with scale 2 -> should produce 123.45
    // 12345 = 0x3039, two bytes
    byte[] bytes = {0x30, 0x39};
    Binary binary = Binary.fromConstantByteArray(bytes);
    BigDecimal result = ParquetValueConverter.binaryToDecimal(binary, 5, 2);
    assertEquals(
        0, new BigDecimal("123.45").compareTo(result), "Expected 123.45 but got " + result);
  }

  @Test
  void binaryToDecimal_negativeValue() {
    // -12345 in two's complement (2 bytes): 0xCFC7
    // -12345 with scale 2 -> -123.45
    byte[] bytes = {(byte) 0xCF, (byte) 0xC7};
    Binary binary = Binary.fromConstantByteArray(bytes);
    BigDecimal result = ParquetValueConverter.binaryToDecimal(binary, 5, 2);
    assertEquals(
        0, new BigDecimal("-123.45").compareTo(result), "Expected -123.45 but got " + result);
  }

  @Test
  void binaryToDecimal_largePrecision_usesBigInteger() {
    // For precision > 18, the BigInteger path is used
    // Encode 12345 with scale 2 in big-endian bytes
    byte[] bytes = {0x30, 0x39};
    Binary binary = Binary.fromConstantByteArray(bytes);
    BigDecimal result = ParquetValueConverter.binaryToDecimal(binary, 20, 2);
    assertEquals(
        0, new BigDecimal("123.45").compareTo(result), "Expected 123.45 but got " + result);
  }

  @Test
  void binaryToDecimal_precisionLossFixed() {
    // This tests the fix for bug #4: double division precision loss
    // Encode a value that would lose precision with double division
    // 123456789012345 with scale 5 -> 1234567890.12345
    long val = 123456789012345L;
    ByteBuffer buf = ByteBuffer.allocate(8);
    buf.putLong(val);
    // Take only the significant bytes (skip leading zeros)
    byte[] fullBytes = buf.array();
    // Find first non-zero byte
    int start = 0;
    while (start < fullBytes.length - 1 && fullBytes[start] == 0) start++;
    byte[] bytes = new byte[fullBytes.length - start];
    System.arraycopy(fullBytes, start, bytes, 0, bytes.length);

    Binary binary = Binary.fromConstantByteArray(bytes);
    BigDecimal result = ParquetValueConverter.binaryToDecimal(binary, 15, 5);
    BigDecimal expected = new BigDecimal("1234567890.12345");
    assertEquals(
        0,
        expected.compareTo(result),
        "Expected " + expected + " but got " + result + " (precision loss from double division)");
  }

  @Test
  void binaryToDecimal_zeroValue() {
    byte[] bytes = {0x00};
    Binary binary = Binary.fromConstantByteArray(bytes);
    BigDecimal result = ParquetValueConverter.binaryToDecimal(binary, 5, 2);
    assertEquals(0, BigDecimal.ZERO.compareTo(result), "Expected 0 but got " + result);
  }

  // =========================================================================
  // Negative rowIndex guard
  // =========================================================================

  @Test
  void addBinary_negativeRowIndex_doesNothing() {
    IValueMeta valueMeta = mock(IValueMeta.class);
    when(valueMeta.getType()).thenReturn(IValueMeta.TYPE_STRING);

    IRowMeta rowMeta = mock(IRowMeta.class);
    // rowIndex -1 is used to indicate "skip"
    // We need getValueMeta to not fail, but the converter uses rowIndex directly
    // Since rowIndex is -1, the early return should prevent any access
    when(rowMeta.size()).thenReturn(1);

    // Use a valid index for the mock but pass -1 to the converter
    // The converter only calls group.getValueMeta(rowIndex) in constructor,
    // so we need to handle that
    when(rowMeta.getValueMeta(-1)).thenReturn(valueMeta);

    Object[] data = new Object[1];
    RowMetaAndData group = new RowMetaAndData(rowMeta, data);
    ParquetValueConverter converter = new ParquetValueConverter(group, -1, null);

    converter.addBinary(Binary.fromString("test"));
    assertNull(data[0], "Data should not be modified when rowIndex is negative");
  }
}
