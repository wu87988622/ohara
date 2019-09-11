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

package com.island.ohara.it.connector

package object smb {
  protected[smb] val SMB_HOSTNAME_KEY: String = "ohara.it.smb.hostname"
  protected[smb] val SMB_PORT_KEY: String = "ohara.it.smb.port"
  protected[smb] val SMB_USERNAME_KEY: String = "ohara.it.smb.username"
  protected[smb] val SMB_PASSWORD_KEY: String = "ohara.it.smb.password"
  protected[smb] val SMB_SHARE_NAME_KEY: String = "ohara.it.smb.shareName"
}
