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

package com.island.ohara.integration;

import com.island.ohara.common.util.CommonUtil;
import com.island.ohara.common.util.Releasable;
import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

/**
 * a simple embedded ftp server providing 1 writable user. The home folder is based on
 * java.io.tmpdir with prefix - ftp 1) port -> a random port 2) hostname -> "localhost" 3) user -> a
 * writable account 4) password -> a writable account
 *
 * <p>all resources will be released by FtpServer#close(). For example, all data in home folder will
 * be deleted
 *
 * <p>If ohara.it.ftp exists in env variables, local ftp server is not created.
 */
public interface FtpServer extends Releasable {
  String FTP_SERVER = "ohara.it.ftp";

  int NUMBER_OF_SERVERS = 3;

  String hostname();

  int port();

  String user();

  String password();

  /**
   * If the ftp server is in passive mode, the port is used to transfer data
   *
   * @return data port
   */
  List<Integer> dataPorts();

  /** @return true if this ftp server is generated locally. */
  boolean isLocal();

  String absolutePath();

  static Builder builder() {
    return new Builder();
  }

  class Builder {
    private Builder() {}

    private String advertisedHostname = CommonUtil.hostname();
    private String user = "user";
    private String password = "password";
    private Integer controlPort = 0;
    private List<Integer> dataPorts = Collections.singletonList(0);

    @com.island.ohara.common.annotations.Optional("default is local hostname")
    public Builder advertisedHostname(String advertisedHostname) {
      this.advertisedHostname = CommonUtil.requireNonEmpty(advertisedHostname);
      return this;
    }

    @com.island.ohara.common.annotations.Optional("default is user")
    public Builder user(String user) {
      this.user = CommonUtil.requireNonEmpty(user);
      return this;
    }

    @com.island.ohara.common.annotations.Optional("default is password")
    public Builder password(String password) {
      this.password = CommonUtil.requireNonEmpty(password);
      return this;
    }

    @com.island.ohara.common.annotations.Optional("default is random port")
    public Builder controlPort(int controlPort) {
      this.controlPort = CommonUtil.requirePositiveInt(controlPort);
      return this;
    }

    /**
     * set the ports used to translate data. NOTED: the max connection of data is equal to number of
     * data ports.
     *
     * @param dataPorts data ports
     * @return this builder
     */
    @com.island.ohara.common.annotations.Optional("default is single random port")
    public Builder dataPorts(List<Integer> dataPorts) {
      CommonUtil.requireNonEmpty(dataPorts).forEach(CommonUtil::requirePositiveInt);
      this.dataPorts = dataPorts;
      return this;
    }

    private void checkArguments() {
      CommonUtil.requireNonEmpty(advertisedHostname);
      CommonUtil.requireNonEmpty(user);
      CommonUtil.requireNonEmpty(password);
      CommonUtil.requirePositiveInt(controlPort);
      CommonUtil.requireNonEmpty(dataPorts).forEach(CommonUtil::requirePositiveInt);
    }

    public FtpServer build() {
      checkArguments();
      File homeFolder = CommonUtil.createTempDir("ftp");
      PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
      UserManager userManager = userManagerFactory.createUserManager();
      BaseUser _user = new BaseUser();
      _user.setName(user);
      _user.setAuthorities(Collections.singletonList(new WritePermission()));
      _user.setEnabled(true);
      _user.setPassword(password);
      _user.setHomeDirectory(homeFolder.getAbsolutePath());
      try {
        userManager.save(_user);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      ListenerFactory listenerFactory = new ListenerFactory();
      listenerFactory.setPort(controlPort);
      DataConnectionConfigurationFactory connectionConfig =
          new DataConnectionConfigurationFactory();

      List<Integer> availableDataPorts =
          dataPorts.stream().map(CommonUtil::resolvePort).collect(Collectors.toList());

      connectionConfig.setActiveEnabled(false);
      connectionConfig.setPassiveExternalAddress(advertisedHostname);
      connectionConfig.setPassivePorts(mkPortString(availableDataPorts));
      listenerFactory.setDataConnectionConfiguration(
          connectionConfig.createDataConnectionConfiguration());

      Listener listener = listenerFactory.createListener();
      FtpServerFactory factory = new FtpServerFactory();
      factory.setUserManager(userManager);
      factory.addListener("default", listener);
      org.apache.ftpserver.FtpServer server = factory.createServer();
      try {
        server.start();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return new FtpServer() {

        @Override
        public void close() {
          server.stop();
          CommonUtil.deleteFiles(homeFolder);
        }

        @Override
        public String hostname() {
          return CommonUtil.hostname();
        }

        @Override
        public int port() {
          return listener.getPort();
        }

        @Override
        public String user() {
          return _user.getName();
        }

        @Override
        public String password() {
          return _user.getPassword();
        }

        @Override
        public List<Integer> dataPorts() {
          return Stream.of(connectionConfig.getPassivePorts().split(","))
              .map(Integer::valueOf)
              .collect(Collectors.toList());
        }

        @Override
        public boolean isLocal() {
          return true;
        }

        @Override
        public String absolutePath() {
          return homeFolder.getAbsolutePath();
        }
      };
    }
  }

  static FtpServer of() {
    return of(System.getenv(FTP_SERVER));
  }

  static FtpServer of(String ftpString) {
    return Optional.ofNullable(ftpString)
        .map(
            f -> {
              String user;
              String password;
              String host;
              int port;

              try {
                user = ftpString.split(":")[0];
                password = ftpString.split("@")[0].split(":")[1];
                host = ftpString.split("@")[1].split(":")[0];
                port = Integer.parseInt(ftpString.split("@")[1].split(":")[1]);
              } catch (Exception e) {
                throw new IllegalArgumentException("invalid value from " + FTP_SERVER, e);
              }
              // FtpServerInfo ftpServerInfo = parseString(f);
              return (FtpServer)
                  new FtpServer() {
                    @Override
                    public void close() {
                      // Nothing
                    }

                    @Override
                    public String hostname() {
                      return host;
                    }

                    @Override
                    public int port() {
                      return port;
                    }

                    @Override
                    public String user() {
                      return user;
                    }

                    @Override
                    public String password() {
                      return password;
                    }

                    @Override
                    public List<Integer> dataPorts() {
                      throw new UnsupportedOperationException(
                          "TODO: can't get data port from actual ftp server");
                    }

                    @Override
                    public boolean isLocal() {
                      return false;
                    }

                    @Override
                    public String absolutePath() {
                      return "/";
                    }
                  };
            })
        .orElseGet(
            () ->
                builder()
                    .advertisedHostname(CommonUtil.hostname())
                    .controlPort(0)
                    .dataPorts(
                        IntStream.range(0, NUMBER_OF_SERVERS)
                            .mapToObj(x -> 0)
                            .collect(Collectors.toList()))
                    .build());
  }

  static String mkPortString(List<Integer> ports) {
    return ports.stream().map(String::valueOf).collect(Collectors.joining(","));
  }

  String ADVERTISED_HOSTNAME = "--hostname";
  String USER = "--user";
  String PASSWORD = "--password";
  String CONTROL_PORT = "--controlPort";
  String DATA_PORTS = "--dataPorts";
  String TTL = "--ttl";
  String USAGE =
      String.join(
          " ",
          Arrays.asList(
              ADVERTISED_HOSTNAME,
              USER,
              PASSWORD,
              CONTROL_PORT,
              DATA_PORTS,
              "(form: 12345,12346 or 12345-12346)",
              TTL));

  static void start(String[] args, Consumer<FtpServer> consumer) throws InterruptedException {
    String advertisedHostname = CommonUtil.hostname();
    String user = "user";
    String password = "password";
    int controlPort = 0;
    List<Integer> dataPorts = Arrays.asList(0, 0, 0);
    int ttl = Integer.MAX_VALUE;
    if (args.length % 2 != 0) throw new IllegalArgumentException(USAGE);
    for (int i = 0; i < args.length; i += 2) {
      String value = args[i + 1];
      switch (args[i]) {
        case ADVERTISED_HOSTNAME:
          advertisedHostname = value;
          break;
        case USER:
          user = value;
          break;
        case PASSWORD:
          password = value;
          break;
        case CONTROL_PORT:
          controlPort = Integer.valueOf(value);
          break;
        case DATA_PORTS:
          if (value.startsWith("-"))
            throw new IllegalArgumentException("dataPorts must be positive");
          else if (value.contains("-"))
            dataPorts =
                IntStream.range(
                        Integer.valueOf(value.split("-")[0]),
                        Integer.valueOf(value.split("-")[1]) + 1)
                    .boxed()
                    .collect(Collectors.toList());
          else
            dataPorts =
                Stream.of(value.split(",")).map(Integer::valueOf).collect(Collectors.toList());
          break;
        case TTL:
          ttl = Integer.valueOf(value);
          break;
        default:
          throw new IllegalArgumentException("unknown key:" + args[i] + " " + USAGE);
      }
    }
    try (FtpServer ftp =
        FtpServer.builder()
            .advertisedHostname(advertisedHostname)
            .user(user)
            .password(password)
            .controlPort(controlPort)
            .dataPorts(dataPorts)
            .build()) {
      System.out.println(
          String.join(
              " ",
              Arrays.asList(
                  "user:",
                  ftp.user(),
                  "password:",
                  ftp.password(),
                  "hostname:",
                  ftp.hostname(),
                  "port:",
                  String.valueOf(ftp.port()),
                  "data ports:",
                  ftp.dataPorts().stream().map(String::valueOf).collect(Collectors.joining(",")),
                  "absolutePath:",
                  ftp.absolutePath())));
      consumer.accept(ftp);
      TimeUnit.SECONDS.sleep(ttl);
    }
  }

  static void main(String[] args) throws InterruptedException {
    start(args, ftp -> {});
  }
}
