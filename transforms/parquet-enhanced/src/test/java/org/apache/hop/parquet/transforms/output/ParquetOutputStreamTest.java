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
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;

class ParquetOutputStreamTest {

  @Test
  void testWriteSingleByteIncrementsPosition() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ParquetOutputStream pos = new ParquetOutputStream(baos);

    assertEquals(0, pos.getPos());
    pos.write(42);
    assertEquals(1, pos.getPos());
  }

  @Test
  void testWriteByteArrayIncrementsPositionByLength() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ParquetOutputStream pos = new ParquetOutputStream(baos);

    byte[] data = new byte[] {1, 2, 3, 4, 5};
    pos.write(data, 0, data.length);
    assertEquals(5, pos.getPos());
  }

  @Test
  void testWriteByteArrayDelegatesToWriteWithOffsetAndLength() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ParquetOutputStream pos = new ParquetOutputStream(baos);

    byte[] data = new byte[] {10, 20, 30};
    pos.write(data);
    assertEquals(3, pos.getPos());
    assertArrayEquals(data, baos.toByteArray());
  }

  @Test
  void testCumulativePosition() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ParquetOutputStream pos = new ParquetOutputStream(baos);

    pos.write(1);
    pos.write(new byte[] {2, 3}, 0, 2);
    pos.write(4);
    assertEquals(4, pos.getPos());
  }

  @Test
  void testGetPosInitiallyZero() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ParquetOutputStream pos = new ParquetOutputStream(baos);
    assertEquals(0, pos.getPos());
  }

  @Test
  void testFlushDelegatesToUnderlyingStream() throws IOException {
    OutputStream mockStream = mock(OutputStream.class);
    ParquetOutputStream pos = new ParquetOutputStream(mockStream);

    pos.flush();
    verify(mockStream).flush();
  }

  @Test
  void testCloseDelegatesToUnderlyingStream() throws IOException {
    OutputStream mockStream = mock(OutputStream.class);
    ParquetOutputStream pos = new ParquetOutputStream(mockStream);

    pos.close();
    verify(mockStream).close();
  }

  @Test
  void testPositionNotIncrementedOnWriteSingleByteFailure() throws IOException {
    OutputStream mockStream = mock(OutputStream.class);
    doThrow(new IOException("write failed")).when(mockStream).write(anyInt());

    ParquetOutputStream pos = new ParquetOutputStream(mockStream);
    assertEquals(0, pos.getPos());

    assertThrows(IOException.class, () -> pos.write(42));
    assertEquals(0, pos.getPos());
  }

  @Test
  void testPositionNotIncrementedOnWriteByteArrayFailure() throws IOException {
    OutputStream mockStream = mock(OutputStream.class);
    doThrow(new IOException("write failed"))
        .when(mockStream)
        .write(any(byte[].class), anyInt(), anyInt());

    ParquetOutputStream pos = new ParquetOutputStream(mockStream);
    assertEquals(0, pos.getPos());

    byte[] data = new byte[] {1, 2, 3};
    assertThrows(IOException.class, () -> pos.write(data, 0, data.length));
    assertEquals(0, pos.getPos());
  }
}
