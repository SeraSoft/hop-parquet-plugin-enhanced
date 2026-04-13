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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.hop.core.RowMetaAndData;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParquetWriteSupportTest {

  @Mock private RecordConsumer recordConsumer;
  @Mock private MessageType messageType;
  @Mock private Schema avroSchema;

  private ParquetWriteSupport writeSupport;

  @BeforeEach
  void setUp() {
    // Default setup with empty fields; individual tests override as needed
  }

  private ParquetWriteSupport createWriteSupport(
      List<ParquetField> fields, List<Integer> sourceFieldIndexes) {
    ParquetWriteSupport ws =
        new ParquetWriteSupport(messageType, avroSchema, sourceFieldIndexes, fields);
    ws.prepareForWrite(recordConsumer);
    return ws;
  }

  private RowMetaAndData createRow(IRowMeta rowMeta, Object... data) {
    return new RowMetaAndData(rowMeta, data);
  }

  private IValueMeta mockValueMeta(int type) throws Exception {
    IValueMeta vm = mock(IValueMeta.class);
    lenient().when(vm.getType()).thenReturn(type);
    lenient().when(vm.isNull(null)).thenReturn(true);
    return vm;
  }

  private IRowMeta mockRowMeta(IValueMeta... valueMetas) {
    IRowMeta rowMeta = mock(IRowMeta.class);
    for (int i = 0; i < valueMetas.length; i++) {
      lenient().when(rowMeta.getValueMeta(i)).thenReturn(valueMetas[i]);
    }
    return rowMeta;
  }

  @Test
  void writeIntegerFieldCallsAddLong() throws Exception {
    IValueMeta vm = mockValueMeta(IValueMeta.TYPE_INTEGER);
    when(vm.isNull(42L)).thenReturn(false);
    when(vm.getInteger(42L)).thenReturn(42L);

    IRowMeta rowMeta = mockRowMeta(vm);
    RowMetaAndData row = createRow(rowMeta, 42L);

    writeSupport =
        createWriteSupport(
            Collections.singletonList(new ParquetField("src", "tgt")),
            Collections.singletonList(0));

    writeSupport.write(row);

    verify(recordConsumer).startMessage();
    verify(recordConsumer).startField("tgt", 0);
    verify(recordConsumer).addLong(42L);
    verify(recordConsumer).endField("tgt", 0);
    verify(recordConsumer).endMessage();
  }

  @Test
  void writeNumberFieldCallsAddDouble() throws Exception {
    IValueMeta vm = mockValueMeta(IValueMeta.TYPE_NUMBER);
    when(vm.isNull(3.14)).thenReturn(false);
    when(vm.getNumber(3.14)).thenReturn(3.14);

    IRowMeta rowMeta = mockRowMeta(vm);
    RowMetaAndData row = createRow(rowMeta, 3.14);

    writeSupport =
        createWriteSupport(
            Collections.singletonList(new ParquetField("src", "tgt")),
            Collections.singletonList(0));

    writeSupport.write(row);

    verify(recordConsumer).startMessage();
    verify(recordConsumer).startField("tgt", 0);
    verify(recordConsumer).addDouble(3.14);
    verify(recordConsumer).endField("tgt", 0);
    verify(recordConsumer).endMessage();
  }

  @Test
  void writeBooleanFieldCallsAddBoolean() throws Exception {
    IValueMeta vm = mockValueMeta(IValueMeta.TYPE_BOOLEAN);
    when(vm.isNull(true)).thenReturn(false);
    when(vm.getBoolean(true)).thenReturn(true);

    IRowMeta rowMeta = mockRowMeta(vm);
    RowMetaAndData row = createRow(rowMeta, true);

    writeSupport =
        createWriteSupport(
            Collections.singletonList(new ParquetField("src", "tgt")),
            Collections.singletonList(0));

    writeSupport.write(row);

    verify(recordConsumer).startMessage();
    verify(recordConsumer).startField("tgt", 0);
    verify(recordConsumer).addBoolean(true);
    verify(recordConsumer).endField("tgt", 0);
    verify(recordConsumer).endMessage();
  }

  @Test
  void writeDateFieldCallsAddLongWithEpochMillis() throws Exception {
    Date testDate = new Date(1700000000000L);
    IValueMeta vm = mockValueMeta(IValueMeta.TYPE_DATE);
    when(vm.isNull(testDate)).thenReturn(false);
    when(vm.getDate(testDate)).thenReturn(testDate);

    IRowMeta rowMeta = mockRowMeta(vm);
    RowMetaAndData row = createRow(rowMeta, testDate);

    writeSupport =
        createWriteSupport(
            Collections.singletonList(new ParquetField("src", "tgt")),
            Collections.singletonList(0));

    writeSupport.write(row);

    verify(recordConsumer).startMessage();
    verify(recordConsumer).startField("tgt", 0);
    verify(recordConsumer).addLong(1700000000000L);
    verify(recordConsumer).endField("tgt", 0);
    verify(recordConsumer).endMessage();
  }

  @Test
  void writeBinaryFieldCallsAddBinary() throws Exception {
    byte[] data = new byte[] {0x01, 0x02, 0x03};
    IValueMeta vm = mockValueMeta(IValueMeta.TYPE_BINARY);
    when(vm.isNull(data)).thenReturn(false);
    when(vm.getBinary(data)).thenReturn(data);

    IRowMeta rowMeta = mockRowMeta(vm);
    RowMetaAndData row = createRow(rowMeta, (Object) data);

    writeSupport =
        createWriteSupport(
            Collections.singletonList(new ParquetField("src", "tgt")),
            Collections.singletonList(0));

    writeSupport.write(row);

    verify(recordConsumer).startMessage();
    verify(recordConsumer).startField("tgt", 0);
    verify(recordConsumer).addBinary(Binary.fromConstantByteArray(data));
    verify(recordConsumer).endField("tgt", 0);
    verify(recordConsumer).endMessage();
  }

  @Test
  void writeBigNumberFieldCallsAddBinaryWithStringRepresentation() throws Exception {
    BigDecimal bigDecimal = new BigDecimal("12345.6789");
    IValueMeta vm = mockValueMeta(IValueMeta.TYPE_BIGNUMBER);
    when(vm.isNull(bigDecimal)).thenReturn(false);
    when(vm.getString(bigDecimal)).thenReturn("12345.6789");

    IRowMeta rowMeta = mockRowMeta(vm);
    RowMetaAndData row = createRow(rowMeta, bigDecimal);

    writeSupport =
        createWriteSupport(
            Collections.singletonList(new ParquetField("src", "tgt")),
            Collections.singletonList(0));

    writeSupport.write(row);

    verify(recordConsumer).startMessage();
    verify(recordConsumer).startField("tgt", 0);
    verify(recordConsumer).addBinary(Binary.fromString("12345.6789"));
    verify(recordConsumer).endField("tgt", 0);
    verify(recordConsumer).endMessage();
  }

  @Test
  void writeStringFieldCallsAddBinaryWithString() throws Exception {
    IValueMeta vm = mockValueMeta(IValueMeta.TYPE_STRING);
    when(vm.isNull("hello")).thenReturn(false);
    when(vm.getString("hello")).thenReturn("hello");

    IRowMeta rowMeta = mockRowMeta(vm);
    RowMetaAndData row = createRow(rowMeta, "hello");

    writeSupport =
        createWriteSupport(
            Collections.singletonList(new ParquetField("src", "tgt")),
            Collections.singletonList(0));

    writeSupport.write(row);

    verify(recordConsumer).startMessage();
    verify(recordConsumer).startField("tgt", 0);
    verify(recordConsumer).addBinary(Binary.fromString("hello"));
    verify(recordConsumer).endField("tgt", 0);
    verify(recordConsumer).endMessage();
  }

  @Test
  void unknownTypeDefaultsToStringHandling() throws Exception {
    // TYPE_SERIALIZABLE (8) is not explicitly handled, should fall through to default (STRING)
    IValueMeta vm = mockValueMeta(IValueMeta.TYPE_SERIALIZABLE);
    when(vm.isNull("serialized")).thenReturn(false);
    when(vm.getString("serialized")).thenReturn("serialized");

    IRowMeta rowMeta = mockRowMeta(vm);
    RowMetaAndData row = createRow(rowMeta, "serialized");

    writeSupport =
        createWriteSupport(
            Collections.singletonList(new ParquetField("src", "tgt")),
            Collections.singletonList(0));

    writeSupport.write(row);

    verify(recordConsumer).startMessage();
    verify(recordConsumer).startField("tgt", 0);
    verify(recordConsumer).addBinary(Binary.fromString("serialized"));
    verify(recordConsumer).endField("tgt", 0);
    verify(recordConsumer).endMessage();
  }

  @Test
  void nullValueSkipsFieldEntirely() throws Exception {
    IValueMeta vm = mockValueMeta(IValueMeta.TYPE_STRING);
    when(vm.isNull(null)).thenReturn(true);

    IRowMeta rowMeta = mockRowMeta(vm);
    RowMetaAndData row = createRow(rowMeta, (Object) null);

    writeSupport =
        createWriteSupport(
            Collections.singletonList(new ParquetField("src", "tgt")),
            Collections.singletonList(0));

    writeSupport.write(row);

    verify(recordConsumer).startMessage();
    verify(recordConsumer).endMessage();
    verify(recordConsumer, never()).startField(any(), anyInt());
    verify(recordConsumer, never()).endField(any(), anyInt());
    verify(recordConsumer, never()).addBinary(any());
    verify(recordConsumer, never()).addLong(anyLong());
    verify(recordConsumer, never()).addDouble(anyDouble());
    verify(recordConsumer, never()).addBoolean(anyBoolean());
  }

  @Test
  void mixOfNullAndNonNullFieldsWritesOnlyNonNullValues() throws Exception {
    // Field 0: non-null string
    IValueMeta vmString = mockValueMeta(IValueMeta.TYPE_STRING);
    when(vmString.isNull("hello")).thenReturn(false);
    when(vmString.getString("hello")).thenReturn("hello");

    // Field 1: null integer
    IValueMeta vmInt = mockValueMeta(IValueMeta.TYPE_INTEGER);
    when(vmInt.isNull(null)).thenReturn(true);

    // Field 2: non-null number
    IValueMeta vmNum = mockValueMeta(IValueMeta.TYPE_NUMBER);
    when(vmNum.isNull(9.99)).thenReturn(false);
    when(vmNum.getNumber(9.99)).thenReturn(9.99);

    IRowMeta rowMeta = mockRowMeta(vmString, vmInt, vmNum);
    RowMetaAndData row = createRow(rowMeta, "hello", null, 9.99);

    List<ParquetField> fields =
        Arrays.asList(
            new ParquetField("src1", "tgt1"),
            new ParquetField("src2", "tgt2"),
            new ParquetField("src3", "tgt3"));
    List<Integer> indexes = Arrays.asList(0, 1, 2);

    writeSupport = createWriteSupport(fields, indexes);
    writeSupport.write(row);

    // Field 0 written
    verify(recordConsumer).startField("tgt1", 0);
    verify(recordConsumer).addBinary(Binary.fromString("hello"));
    verify(recordConsumer).endField("tgt1", 0);

    // Field 1 skipped (null)
    verify(recordConsumer, never()).startField(eq("tgt2"), eq(1));
    verify(recordConsumer, never()).endField(eq("tgt2"), eq(1));

    // Field 2 written
    verify(recordConsumer).startField("tgt3", 2);
    verify(recordConsumer).addDouble(9.99);
    verify(recordConsumer).endField("tgt3", 2);

    verify(recordConsumer).startMessage();
    verify(recordConsumer).endMessage();
  }

  @Test
  void writeCallsStartMessageAndEndMessage() throws Exception {
    // Even with no fields, startMessage and endMessage should be called
    writeSupport = createWriteSupport(Collections.emptyList(), Collections.emptyList());

    IRowMeta rowMeta = mockRowMeta();
    RowMetaAndData row = createRow(rowMeta);

    writeSupport.write(row);

    verify(recordConsumer).startMessage();
    verify(recordConsumer).endMessage();
  }

  @Test
  void multipleFieldsOfDifferentTypesAreWrittenCorrectly() throws Exception {
    // Integer field
    IValueMeta vmInt = mockValueMeta(IValueMeta.TYPE_INTEGER);
    when(vmInt.isNull(100L)).thenReturn(false);
    when(vmInt.getInteger(100L)).thenReturn(100L);

    // Boolean field
    IValueMeta vmBool = mockValueMeta(IValueMeta.TYPE_BOOLEAN);
    when(vmBool.isNull(false)).thenReturn(false);
    when(vmBool.getBoolean(false)).thenReturn(false);

    // String field
    IValueMeta vmStr = mockValueMeta(IValueMeta.TYPE_STRING);
    when(vmStr.isNull("test")).thenReturn(false);
    when(vmStr.getString("test")).thenReturn("test");

    IRowMeta rowMeta = mockRowMeta(vmInt, vmBool, vmStr);
    RowMetaAndData row = createRow(rowMeta, 100L, false, "test");

    List<ParquetField> fields =
        Arrays.asList(
            new ParquetField("intSrc", "intTgt"),
            new ParquetField("boolSrc", "boolTgt"),
            new ParquetField("strSrc", "strTgt"));
    List<Integer> indexes = Arrays.asList(0, 1, 2);

    writeSupport = createWriteSupport(fields, indexes);
    writeSupport.write(row);

    verify(recordConsumer).startField("intTgt", 0);
    verify(recordConsumer).addLong(100L);
    verify(recordConsumer).endField("intTgt", 0);

    verify(recordConsumer).startField("boolTgt", 1);
    verify(recordConsumer).addBoolean(false);
    verify(recordConsumer).endField("boolTgt", 1);

    verify(recordConsumer).startField("strTgt", 2);
    verify(recordConsumer).addBinary(Binary.fromString("test"));
    verify(recordConsumer).endField("strTgt", 2);
  }
}
