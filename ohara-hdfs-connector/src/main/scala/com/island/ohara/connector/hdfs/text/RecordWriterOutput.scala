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

package com.island.ohara.connector.hdfs.text

import com.island.ohara.common.data.{Column, Row}

/**
  * This abstract to define how to write data to file
  */
abstract class RecordWriterOutput {

  /**
    * The data write to file
    * @param row
    */
  def write(isHeader: Boolean, schema: Seq[Column], row: Row): Unit

  /**
    * close file connection
    */
  def close(): Unit
}
