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

package com.island.ohara.it.connector.smb

case class ITSmbProps(hostname: String, port: Int, username: String, password: String, shareName: String) {
  def toMap: Map[String, String] =
    Map(
      IT_SMB_HOSTNAME_KEY   -> hostname,
      IT_SMB_PORT_KEY       -> port.toString,
      IT_SMB_USERNAME_KEY   -> username,
      IT_SMB_PASSWORD_KEY   -> password,
      IT_SMB_SHARE_NAME_KEY -> shareName
    ).filter(_._2.nonEmpty)
}

object ITSmbProps {
  def apply(variables: Map[String, String]): ITSmbProps = {
    val hostname  = variables.get(IT_SMB_HOSTNAME_KEY)
    val port      = variables.get(IT_SMB_PORT_KEY)
    val username  = variables.get(IT_SMB_USERNAME_KEY)
    val password  = variables.get(IT_SMB_PASSWORD_KEY)
    val shareName = variables.get(IT_SMB_SHARE_NAME_KEY)

    if (hostname.isEmpty || port.isEmpty || username.isEmpty || password.isEmpty || shareName.isEmpty) {
      throw new IllegalArgumentException(
        s"please setting $IT_SMB_HOSTNAME_KEY, $IT_SMB_PORT_KEY, $IT_SMB_USERNAME_KEY, $IT_SMB_PASSWORD_KEY and $IT_SMB_SHARE_NAME_KEY properties"
      )
    }

    ITSmbProps(
      hostname = hostname.get,
      port = Integer.parseInt(port.get),
      username = username.get,
      password = password.get,
      shareName = shareName.get
    )
  }
}
