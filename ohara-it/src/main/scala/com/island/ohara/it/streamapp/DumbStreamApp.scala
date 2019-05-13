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

package com.island.ohara.it.streamapp
import com.island.ohara.common.data.Row
import com.island.ohara.streams.{OStream, StreamApp}

/**
  * This is a sample streamApp that will do nothing
  * It is not placed at test scope since we need jar when tests manually.
  */
class DumbStreamApp extends StreamApp {

  override def start(): Unit = {
    val ostream: OStream[Row] = OStream.builder().toOharaEnvStream

    // do nothing but only start streamApp
    ostream.start()
  }
}
