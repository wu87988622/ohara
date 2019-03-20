/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.kafka.connector.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.ohara.common.annotations.VisibleForTesting;
import com.island.ohara.common.util.CommonUtils;
import java.io.IOException;
import java.util.Objects;

@VisibleForTesting
final class JsonUtils {

  static <T> T toObject(String string, TypeReference<T> ref) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readValue(CommonUtils.requireNonEmpty(string), ref);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  static String toString(Object obj) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(Objects.requireNonNull(obj));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private JsonUtils() {}
}
