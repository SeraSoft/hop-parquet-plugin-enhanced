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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.hop.core.HopEnvironment;
import org.apache.hop.core.exception.HopException;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ParquetRecordMaterializerTest {

  @BeforeAll
  static void setUp() throws HopException {
    HopEnvironment.init();
  }

  private static final String SCHEMA_STRING =
      "message test_schema {\n"
          + "  required binary name (UTF8);\n"
          + "  required int32 age;\n"
          + "  optional double score;\n"
          + "}";

  private MessageType createTestSchema() {
    return MessageTypeParser.parseMessageType(SCHEMA_STRING);
  }

  @Test
  void constructorWithExistingFieldsSucceeds() {
    MessageType schema = createTestSchema();
    List<ParquetField> fields = new ArrayList<>();
    fields.add(new ParquetField("name", "name_out", "String", null, null, null));
    fields.add(new ParquetField("age", "age_out", "Integer", null, null, null));

    ParquetRecordMaterializer materializer = new ParquetRecordMaterializer(schema, fields);

    assertNotNull(materializer.getRootConverter());
    assertNotNull(materializer.getCurrentRecord());
  }

  @Test
  void constructorWithNonExistingFieldThrowsException() {
    MessageType schema = createTestSchema();
    List<ParquetField> fields = new ArrayList<>();
    fields.add(new ParquetField("non_existent", "out", "String", null, null, null));

    // Parquet's getFieldIndex throws InvalidRecordException for missing fields
    assertThrows(RuntimeException.class, () -> new ParquetRecordMaterializer(schema, fields));
  }

  @Test
  void constructorWithNonExistingFieldIncludesFieldNameInMessage() {
    MessageType schema = createTestSchema();
    List<ParquetField> fields = new ArrayList<>();
    fields.add(new ParquetField("missing_field", "out", "String", null, null, null));

    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> new ParquetRecordMaterializer(schema, fields));
    assertTrue(
        ex.getMessage().contains("missing_field"),
        "Exception message should mention the missing field name");
  }

  @Test
  void constructorWithEmptyFieldsList() {
    MessageType schema = createTestSchema();
    List<ParquetField> fields = Collections.emptyList();

    ParquetRecordMaterializer materializer = new ParquetRecordMaterializer(schema, fields);

    assertNotNull(materializer.getRootConverter());
    assertNotNull(materializer.getCurrentRecord());
  }

  @Test
  void constructorWithAllSchemaFields() {
    MessageType schema = createTestSchema();
    List<ParquetField> fields = new ArrayList<>();
    fields.add(new ParquetField("name", "name_out", "String", null, null, null));
    fields.add(new ParquetField("age", "age_out", "Integer", null, null, null));
    fields.add(new ParquetField("score", "score_out", "Number", null, null, null));

    ParquetRecordMaterializer materializer = new ParquetRecordMaterializer(schema, fields);

    assertNotNull(materializer.getRootConverter());
  }

  @Test
  void constructorFailsIfSecondFieldMissing() {
    MessageType schema = createTestSchema();
    List<ParquetField> fields = new ArrayList<>();
    fields.add(new ParquetField("name", "name_out", "String", null, null, null));
    fields.add(new ParquetField("does_not_exist", "out", "String", null, null, null));

    assertThrows(RuntimeException.class, () -> new ParquetRecordMaterializer(schema, fields));
  }
}
