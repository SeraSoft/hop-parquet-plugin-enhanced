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

import org.junit.jupiter.api.Test;

class ParquetVersionTest {

  @Test
  void testGetVersionFromDescriptionVersion1() {
    assertEquals(ParquetVersion.Version1, ParquetVersion.getVersionFromDescription("Parquet 1.0"));
  }

  @Test
  void testGetVersionFromDescriptionVersion2() {
    assertEquals(ParquetVersion.Version2, ParquetVersion.getVersionFromDescription("Parquet 2.0"));
  }

  @Test
  void testGetVersionFromDescriptionCaseInsensitive() {
    assertEquals(ParquetVersion.Version1, ParquetVersion.getVersionFromDescription("parquet 1.0"));
    assertEquals(ParquetVersion.Version2, ParquetVersion.getVersionFromDescription("parquet 2.0"));
    assertEquals(ParquetVersion.Version2, ParquetVersion.getVersionFromDescription("PARQUET 2.0"));
  }

  @Test
  void testGetVersionFromDescriptionNullFallsBackToVersion1() {
    // null description cannot match any version, so it falls back to Version1
    assertEquals(ParquetVersion.Version1, ParquetVersion.getVersionFromDescription(null));
  }

  @Test
  void testGetVersionFromDescriptionEmptyStringFallsBackToVersion1() {
    assertEquals(ParquetVersion.Version1, ParquetVersion.getVersionFromDescription(""));
  }

  @Test
  void testGetVersionFromDescriptionInvalidFallsBackToVersion1() {
    assertEquals(ParquetVersion.Version1, ParquetVersion.getVersionFromDescription("invalid"));
  }

  @Test
  void testGetDescriptionsReturnsAllDescriptions() {
    String[] descriptions = ParquetVersion.getDescriptions();
    assertEquals(2, descriptions.length);
    assertEquals("Parquet 1.0", descriptions[0]);
    assertEquals("Parquet 2.0", descriptions[1]);
  }

  @Test
  void testGetCode() {
    assertEquals("1.0", ParquetVersion.Version1.getCode());
    assertEquals("2.0", ParquetVersion.Version2.getCode());
  }

  @Test
  void testGetDescription() {
    assertEquals("Parquet 1.0", ParquetVersion.Version1.getDescription());
    assertEquals("Parquet 2.0", ParquetVersion.Version2.getDescription());
  }
}
