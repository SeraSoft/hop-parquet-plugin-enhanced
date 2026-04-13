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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.apache.hop.core.exception.HopException;
import org.apache.parquet.io.SeekableInputStream;

public class ParquetInputStream extends SeekableInputStream {

  private final byte[] bytes;
  private final ByteArrayInputStream inputStream;
  private long position;

  /**
   * Read the input stream into memory to get a seekable input stream...
   *
   * @param inputStream
   * @throws HopException
   */
  public ParquetInputStream(InputStream inputStream) throws IOException {

    try {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      int bytesRead;
      byte[] chunk = new byte[64 * 1024];
      while ((bytesRead = inputStream.read(chunk, 0, chunk.length)) != -1) {
        buffer.write(chunk, 0, bytesRead);
      }
      this.bytes = buffer.toByteArray();
      this.inputStream = new ByteArrayInputStream(bytes);
    } catch (IOException e) {
      throw new IOException("Unable to read input stream data into memory", e);
    }

    position = 0L;
  }

  @Override
  public int read(byte[] buffer) throws IOException {
    int bytesRead = inputStream.read(buffer);
    if (bytesRead > 0) {
      position += bytesRead;
    }
    return bytesRead;
  }

  @Override
  public int read(byte[] bytes, int offset, int length) throws IOException {
    int bytesRead = inputStream.read(bytes, offset, length);
    if (bytesRead > 0) {
      position += bytesRead;
    }
    return bytesRead;
  }

  @Override
  public long skip(long n) throws IOException {
    long skipped = inputStream.skip(n);
    position += skipped;
    return skipped;
  }

  @Override
  public int available() throws IOException {
    return inputStream.available();
  }

  @Override
  public void close() throws IOException {
    inputStream.close();
  }

  @Override
  public synchronized void mark(int readLimit) {
    inputStream.mark(readLimit);
  }

  @Override
  public synchronized void reset() throws IOException {
    inputStream.reset();
    position = 0;
  }

  @Override
  public boolean markSupported() {
    return false;
  }

  @Override
  public long getPos() throws IOException {
    return position;
  }

  @Override
  public void seek(long pos) throws IOException {
    inputStream.reset();
    long remaining = pos;
    while (remaining > 0) {
      long skipped = inputStream.skip(remaining);
      if (skipped <= 0) {
        throw new EOFException(
            "Cannot seek to position " + pos + " in stream of " + bytes.length + " bytes");
      }
      remaining -= skipped;
    }
    position = pos;
  }

  @Override
  public void readFully(byte[] buffer) throws IOException {
    readFully(buffer, 0, buffer.length);
  }

  @Override
  public void readFully(byte[] buffer, int offset, int length) throws IOException {
    int totalRead = 0;
    while (totalRead < length) {
      int read = inputStream.read(buffer, offset + totalRead, length - totalRead);
      if (read < 0) {
        throw new EOFException(
            "Reached end of stream after reading " + totalRead + " bytes, expected " + length);
      }
      totalRead += read;
    }
    position += totalRead;
  }

  @Override
  public int read(ByteBuffer byteBuffer) throws IOException {
    int read = inputStream.read(byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining());
    if (read > 0) {
      position += read;
      byteBuffer.position(byteBuffer.position() + read);
    }
    return read;
  }

  @Override
  public void readFully(ByteBuffer byteBuffer) throws IOException {
    int length = byteBuffer.remaining();
    int totalRead = 0;
    while (totalRead < length) {
      int read =
          inputStream.read(
              byteBuffer.array(), byteBuffer.position() + totalRead, length - totalRead);
      if (read < 0) {
        throw new EOFException(
            "Reached end of stream after reading " + totalRead + " bytes, expected " + length);
      }
      totalRead += read;
    }
    byteBuffer.position(byteBuffer.position() + totalRead);
    position += totalRead;
  }

  @Override
  public int read() throws IOException {
    int c = inputStream.read();
    if (c >= 0) {
      position++;
    }
    return c;
  }
}
