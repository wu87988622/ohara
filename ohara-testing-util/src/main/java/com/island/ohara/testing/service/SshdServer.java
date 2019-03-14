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

package com.island.ohara.testing.service;

import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.common.util.Releasable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellCommandFactory;
import org.apache.sshd.server.shell.ProcessShellFactory;

public interface SshdServer extends Releasable {

  /** @return ssh server's hostname */
  String hostname();

  /** @return ssh server's port */
  int port();

  /** @return ssh client's user */
  String user();

  /** @return ssh client's password */
  String password();

  interface CommandHandler {
    /**
     * @param cmd will be executed on ssh server
     * @return true if this handler want to handle the command
     */
    boolean belong(String cmd);

    /**
     * @param cmd will be executed on ssh server
     * @return response of the command
     */
    List<String> execute(String cmd);
  }

  static SshdServer local() {
    return local(0);
  }

  static SshdServer local(int port) {
    return local(port, Collections.emptyList());
  }

  static SshdServer local(int port, List<CommandHandler> handlers) {
    String _user = CommonUtils.randomString();
    String _password = CommonUtils.randomString();
    SshServer sshd = SshServer.setUpDefaultServer();
    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
    sshd.setPasswordAuthenticator(
        (String username, String password, ServerSession session) ->
            username.equals(_user) && password.equals(_password));
    sshd.setShellFactory(new ProcessShellFactory(Arrays.asList("/bin/sh", "-i", "-l")));
    sshd.setCommandFactory(
        (String command) ->
            handlers
                .stream()
                .filter(h -> h.belong(command))
                .findFirst()
                .map(
                    h ->
                        (Command)
                            new Command() {
                              private OutputStream out = null;
                              private OutputStream err = null;
                              private ExitCallback callback = null;

                              @Override
                              public void start(Environment env) {
                                try {
                                  h.execute(command)
                                      .forEach(
                                          s -> {
                                            try {
                                              out.write(s.getBytes());
                                              // TODO: make it configurable...by chia
                                              out.write("\n".getBytes());
                                            } catch (Throwable e) {
                                              throw new RuntimeException(e);
                                            }
                                          });
                                  callback.onExit(0);
                                } catch (Throwable e) {
                                  callback.onExit(2, e.getMessage());
                                }
                              }

                              @Override
                              public void destroy() {
                                Releasable.close(out);
                                Releasable.close(err);
                              }

                              @Override
                              public void setInputStream(InputStream in) {
                                // do nothing
                              }

                              @Override
                              public void setOutputStream(OutputStream out) {
                                this.out = out;
                              }

                              @Override
                              public void setErrorStream(OutputStream err) {
                                this.err = err;
                              }

                              @Override
                              public void setExitCallback(ExitCallback callback) {
                                this.callback = callback;
                              }
                            })
                .orElseGet(() -> ProcessShellCommandFactory.INSTANCE.createCommand(command)));
    sshd.setHost(CommonUtils.hostname());
    sshd.setPort(Math.max(port, 0));
    try {
      sshd.start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new SshdServer() {

      @Override
      public void close() {
        Releasable.close(sshd);
      }

      @Override
      public String hostname() {
        return sshd.getHost();
      }

      @Override
      public int port() {
        return sshd.getPort();
      }

      @Override
      public String user() {
        return _user;
      }

      @Override
      public String password() {
        return _password;
      }
    };
  }
}
