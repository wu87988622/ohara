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

package com.island.ohara.connector

import com.island.ohara.common.setting.SettingDef

package object smb {

  val SMB_HOSTNAME_KEY: String = "smb.hostname"
  val SMB_HOSTNAME_DEFINITION: SettingDef = SettingDef
    .builder()
    .key(SMB_HOSTNAME_KEY)
    .documentation("The hostname of SMB server")
    .valueType(SettingDef.Type.STRING)
    .build()

  val SMB_PORT_KEY: String = "smb.port"
  val SMB_PORT_DEFINITION: SettingDef = SettingDef
    .builder()
    .key(SMB_PORT_KEY)
    .documentation("The port of SMB server")
    .valueType(SettingDef.Type.PORT)
    .build()

  val SMB_USER_KEY: String = "smb.user"
  val SMB_USER_DEFINITION: SettingDef = SettingDef
    .builder()
    .key(SMB_USER_KEY)
    .documentation(
      "The username of SMB server. This account must have read/delete permission of input folder and error folder")
    .valueType(SettingDef.Type.STRING)
    .build()

  val SMB_PASSWORD_KEY: String = "smb.password"
  val SMB_PASSWORD_DEFINITION: SettingDef = SettingDef
    .builder()
    .key(SMB_PASSWORD_KEY)
    .documentation("The password of SMB server.")
    .valueType(SettingDef.Type.STRING)
    .build()

  val SMB_SHARE_NAME_KEY: String = "smb.shareName"
  val SMB_SHARE_NAME_DEFINITION: SettingDef = SettingDef
    .builder()
    .key(SMB_SHARE_NAME_KEY)
    .documentation("The share name of SMB server.")
    .valueType(SettingDef.Type.STRING)
    .build()

  /**
    * the core settings for SmbSource.
    */
  val DEFINITIONS: Seq[SettingDef] = Seq(
    SMB_HOSTNAME_DEFINITION,
    SMB_PORT_DEFINITION,
    SMB_USER_DEFINITION,
    SMB_PASSWORD_DEFINITION,
    SMB_SHARE_NAME_DEFINITION
  )
}
