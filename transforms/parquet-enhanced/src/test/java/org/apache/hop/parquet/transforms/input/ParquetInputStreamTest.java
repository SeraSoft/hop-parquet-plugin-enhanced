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

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParquetInputStreamTest {

  private static final byte[] TEST_DATA = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
  private ParquetInputStream stream;

  @BeforeEach
  void setUp() throws IOException {
    stream = new ParquetInputStream(new ByteArrayInputStream(TEST_DATA));
  }

  // --- Constructor ---

  @Test
  void constructorReadsEntireStream() throws IOException {
    assertEquals(0, stream.getPos());
    assertEquals(TEST_DATA.length, stream.available());
  }

  @Test
  void constructorWithEmptyStream() throws IOException {
    ParquetInputStream empty = new ParquetInputStream(new ByteArrayInputStream(new byte[0]));
    assertEquals(0, empty.getPos());
    assertEquals(0, empty.available());
  }

  // --- read() single byte ---

  @Test
  void readSingleByte() throws IOException {
    int b = stream.read();
    assertEquals(0, b);
    assertEquals(1, stream.getPos());
  }

  @Test
  void readSingleByteAtEofReturnsMinusOneAndDoesNotIncrementPosition() throws IOException {
    ParquetInputStream empty = new ParquetInputStream(new ByteArrayInputStream(new byte[0]));
    int b = empty.read();
    assertEquals(-1, b);
    assertEquals(0, empty.getPos());
  }

  @Test
  void readAllBytesSingly() throws IOException {
    for (int i = 0; i < TEST_DATA.length; i++) {
      assertEquals(i, stream.read());
    }
    assertEquals(TEST_DATA.length, stream.getPos());
    assertEquals(-1, stream.read());
  }

  // --- read(byte[]) ---

  @Test
  void readIntoBuffer() throws IOException {
    byte[] buf = new byte[5];
    int read = stream.read(buf);
    assertEquals(5, read);
    assertEquals(5, stream.getPos());
    assertArrayEquals(new byte[] {0, 1, 2, 3, 4}, buf);
  }

  @Test
  void readIntoBufferLargerThanData() throws IOException {
    byte[] buf = new byte[20];
    int read = stream.read(buf);
    assertEquals(10, read);
    assertEquals(10, stream.getPos());
  }

  @Test
  void readIntoBufferAtEofReturnsMinusOne() throws IOException {
    stream.read(new byte[10]); // consume all
    int read = stream.read(new byte[5]);
    assertEquals(-1, read);
    assertEquals(10, stream.getPos()); // position should NOT have changed
  }

  // --- read(byte[], offset, length) ---

  @Test
  void readWithOffsetAndLength() throws IOException {
    byte[] buf = new byte[10];
    int read = stream.read(buf, 3, 4);
    assertEquals(4, read);
    assertEquals(4, stream.getPos());
    assertEquals(0, buf[3]);
    assertEquals(1, buf[4]);
    assertEquals(2, buf[5]);
    assertEquals(3, buf[6]);
  }

  @Test
  void readWithOffsetAtEofReturnsMinusOne() throws IOException {
    stream.read(new byte[10]); // consume all
    byte[] buf = new byte[10];
    int read = stream.read(buf, 0, 5);
    assertEquals(-1, read);
    assertEquals(10, stream.getPos());
  }

  // --- seek() ---

  @Test
  void seekToBeginning() throws IOException {
    stream.read(new byte[5]);
    stream.seek(0);
    assertEquals(0, stream.getPos());
    assertEquals(0, stream.read());
  }

  @Test
  void seekToMiddle() throws IOException {
    stream.seek(5);
    assertEquals(5, stream.getPos());
    assertEquals(5, stream.read());
  }

  @Test
  void seekToEnd() throws IOException {
    stream.seek(10);
    assertEquals(10, stream.getPos());
    assertEquals(-1, stream.read());
  }

  @Test
  void seekBeyondEndThrowsEOFException() {
    assertThrows(EOFException.class, () -> stream.seek(100));
  }

  @Test
  void seekBackAndForth() throws IOException {
    stream.seek(7);
    assertEquals(7, stream.read());
    stream.seek(2);
    assertEquals(2, stream.read());
    assertEquals(3, stream.getPos());
  }

  // --- readFully(byte[]) ---

  @Test
  void readFullyFillsEntireBuffer() throws IOException {
    byte[] buf = new byte[10];
    stream.readFully(buf);
    assertArrayEquals(TEST_DATA, buf);
    assertEquals(10, stream.getPos());
  }

  @Test
  void readFullyPartialBuffer() throws IOException {
    byte[] buf = new byte[5];
    stream.readFully(buf);
    assertArrayEquals(new byte[] {0, 1, 2, 3, 4}, buf);
    assertEquals(5, stream.getPos());
  }

  @Test
  void readFullyThrowsOnInsufficientData() throws IOException {
    stream.seek(8);
    byte[] buf = new byte[5]; // only 2 bytes left
    assertThrows(EOFException.class, () -> stream.readFully(buf));
  }

  // --- readFully(byte[], offset, length) ---

  @Test
  void readFullyWithOffset() throws IOException {
    byte[] buf = new byte[10];
    stream.readFully(buf, 2, 5);
    assertEquals(5, stream.getPos());
    assertEquals(0, buf[2]);
    assertEquals(4, buf[6]);
  }

  @Test
  void readFullyWithOffsetThrowsOnInsufficientData() throws IOException {
    stream.seek(9);
    byte[] buf = new byte[10];
    assertThrows(EOFException.class, () -> stream.readFully(buf, 0, 5));
  }

  // --- readFully(ByteBuffer) ---

  @Test
  void readFullyByteBuffer() throws IOException {
    ByteBuffer bb = ByteBuffer.allocate(10);
    stream.readFully(bb);
    assertEquals(10, stream.getPos());
    bb.flip();
    for (int i = 0; i < 10; i++) {
      assertEquals(i, bb.get());
    }
  }

  @Test
  void readFullyByteBufferPartial() throws IOException {
    ByteBuffer bb = ByteBuffer.allocate(5);
    stream.readFully(bb);
    assertEquals(5, stream.getPos());
    bb.flip();
    for (int i = 0; i < 5; i++) {
      assertEquals(i, bb.get());
    }
  }

  @Test
  void readFullyByteBufferThrowsOnInsufficientData() throws IOException {
    stream.seek(8);
    ByteBuffer bb = ByteBuffer.allocate(5);
    assertThrows(EOFException.class, () -> stream.readFully(bb));
  }

  // --- read(ByteBuffer) ---

  @Test
  void readByteBuffer() throws IOException {
    ByteBuffer bb = ByteBuffer.allocate(5);
    int read = stream.read(bb);
    assertEquals(5, read);
    assertEquals(5, stream.getPos());
    bb.flip();
    assertEquals(0, bb.get());
    assertEquals(4, bb.get(4));
  }

  @Test
  void readByteBufferAtEof() throws IOException {
    stream.seek(10);
    ByteBuffer bb = ByteBuffer.allocate(5);
    int read = stream.read(bb);
    assertEquals(-1, read);
    assertEquals(10, stream.getPos());
  }

  // --- skip() ---

  @Test
  void skipForward() throws IOException {
    long skipped = stream.skip(3);
    assertEquals(3, skipped);
    assertEquals(3, stream.getPos());
    assertEquals(3, stream.read());
  }

  @Test
  void skipBeyondEnd() throws IOException {
    long skipped = stream.skip(100);
    assertTrue(skipped <= TEST_DATA.length);
    assertEquals(skipped, stream.getPos());
  }

  // --- Position tracking across mixed operations ---

  @Test
  void positionTrackingAcrossMixedOps() throws IOException {
    stream.read(); // pos=1
    stream.read(new byte[2]); // pos=3
    stream.skip(2); // pos=5
    assertEquals(5, stream.getPos());
    assertEquals(5, stream.read()); // pos=6
    assertEquals(6, stream.getPos());
  }

  @Test
  void positionTrackingAfterSeekAndRead() throws IOException {
    stream.read(new byte[10]); // pos=10
    stream.seek(3); // pos=3
    byte[] buf = new byte[4];
    stream.readFully(buf); // pos=7
    assertEquals(7, stream.getPos());
    assertArrayEquals(new byte[] {3, 4, 5, 6}, buf);
  }
}
