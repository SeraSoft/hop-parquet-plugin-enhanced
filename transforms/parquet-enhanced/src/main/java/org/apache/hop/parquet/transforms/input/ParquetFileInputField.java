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

import lombok.Getter;
import lombok.Setter;
import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.value.ValueMetaFactory;
import org.apache.hop.metadata.api.HopMetadataProperty;

public class ParquetFileInputField {
  @HopMetadataProperty(key = "source_field")
  @Getter
  @Setter
  private String sourceField;

  @HopMetadataProperty(key = "target_field")
  @Getter
  @Setter
  private String targetField;

  @HopMetadataProperty(key = "target_type")
  @Getter
  @Setter
  private String targetType;

  @HopMetadataProperty(key = "target_format")
  @Getter
  @Setter
  private String targetFormat;

  @HopMetadataProperty(key = "target_length")
  @Getter
  @Setter
  private int targetLength;

  @HopMetadataProperty(key = "target_precision")
  @Getter
  @Setter
  private int targetPrecision;

  public ParquetFileInputField() {}

  public IValueMeta createValueMeta() throws HopException {
    int type = ValueMetaFactory.getIdForValueMeta(targetType);
    IValueMeta valueMeta = ValueMetaFactory.createValueMeta(targetField, type, targetLength, targetPrecision);
    valueMeta.setConversionMask(targetFormat);
    return valueMeta;
  }
}
