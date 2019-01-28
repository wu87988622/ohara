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

import static com.wix.mysql.EmbeddedMysql.anEmbeddedMysql;
import static com.wix.mysql.config.Charset.UTF8;
import static com.wix.mysql.config.MysqldConfig.aMysqldConfig;
import static com.wix.mysql.distribution.Version.v5_7_latest;

import com.island.ohara.common.util.CommonUtil;
import com.island.ohara.common.util.Releasable;
import com.wix.mysql.EmbeddedMysql;
import com.wix.mysql.config.MysqldConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface Database extends Releasable {

  String DB_SERVER = "ohara.it.db";

  String hostname();

  int port();

  String databaseName();

  String user();

  String password();

  String url();

  Connection connection();

  /** @return true if this database is generated locally. */
  boolean isLocal();

  static Database local(int port) {
    return local(
        CommonUtil.randomString(10),
        CommonUtil.randomString(10),
        CommonUtil.randomString(10),
        port);
  }

  /**
   * create an embedded mysql with specific port
   *
   * @param port bound port
   * @return an embedded mysql
   */
  static Database local(String user, String password, String dbName, int port) {
    MysqldConfig config =
        aMysqldConfig(v5_7_latest)
            .withCharset(UTF8)
            .withUser(user, password)
            .withTimeZone(CommonUtil.timezone())
            .withTimeout(2, TimeUnit.MINUTES)
            .withServerVariable("max_connect_errors", 666)
            .withTempDir(CommonUtil.createTempDir("my_sql").getAbsolutePath())
            .withPort(CommonUtil.resolvePort(port))
            // make mysql use " replace '
            // see https://stackoverflow.com/questions/13884854/mysql-double-quoted-table-names
            .withServerVariable("sql-mode", "ANSI_QUOTES")
            .build();
    EmbeddedMysql mysqld = anEmbeddedMysql(config).addSchema(dbName).start();
    return new Database() {
      private Connection connection = null;

      @Override
      public void close() {
        Releasable.close(connection);
        mysqld.stop();
      }

      @Override
      public String hostname() {
        return CommonUtil.hostname();
      }

      @Override
      public int port() {
        return config.getPort();
      }

      @Override
      public String databaseName() {
        return dbName;
      }

      @Override
      public String user() {
        return config.getUsername();
      }

      @Override
      public String password() {
        return config.getPassword();
      }

      @Override
      public String url() {
        return "jdbc:mysql://" + hostname() + ":" + port() + "/" + databaseName();
      }

      @Override
      public Connection connection() {
        if (connection == null) {
          try {
            connection = DriverManager.getConnection(url(), user(), password());
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
        return connection;
      }

      @Override
      public boolean isLocal() {
        return true;
      }
    };
  }

  static Database of() {
    return of(System.getenv(DB_SERVER));
  }

  static Database of(String dbString) {
    return Optional.ofNullable(dbString)
        .map(
            d -> {
              if (!dbString.startsWith("jdbc"))
                throw new IllegalArgumentException("invalid url:" + dbString);
              String dbInstance = dbString.split(":")[1];
              String user = dbString.split(":")[2];
              String password = dbString.split(":")[3].split("@//")[0];
              String host = dbString.split(":")[3].split("@//")[1];
              int port = Integer.parseInt(dbString.split(":")[4].split("/")[0]);
              String dbName = dbString.split(":")[4].split("/")[1];

              return (Database)
                  new Database() {
                    private Connection connection = null;

                    @Override
                    public void close() {
                      Releasable.close(connection);
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
                    public String databaseName() {
                      return dbName;
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
                    public String url() {
                      return "jdbc:"
                          + dbInstance
                          + "://"
                          + hostname()
                          + ":"
                          + port()
                          + "/"
                          + databaseName();
                    }

                    @Override
                    public Connection connection() {
                      if (connection == null) {
                        try {
                          connection = DriverManager.getConnection(url(), user(), password());
                        } catch (Exception e) {
                          throw new RuntimeException(e);
                        }
                      }
                      return connection;
                    }

                    @Override
                    public boolean isLocal() {
                      return false;
                    }
                  };
            })
        .orElseGet(() -> local(0));
  }

  String USER = "--user";
  String PASSWORD = "--password";
  String DB_NAME = "--dbName";
  String PORT = "--port";
  String TTL = "--ttl";
  String USAGE = String.join(" ", Arrays.asList(USER, PASSWORD, PORT, DB_NAME, TTL));

  static void start(String[] args, Consumer<Database> consumer) throws InterruptedException {
    String user = "user";
    String password = "password";
    String dbName = "ohara";
    int port = -1;
    int ttl = Integer.MAX_VALUE;
    for (int i = 0; i < args.length; i += 2) {
      String value = args[i + 1];
      switch (args[i]) {
        case USER:
          user = value;
          break;
        case PASSWORD:
          password = value;
          break;
        case PORT:
          port = Integer.valueOf(value);
          break;
        case DB_NAME:
          dbName = value;
          break;
        case TTL:
          ttl = Integer.valueOf(value);
          break;
        default:
          throw new IllegalArgumentException(USAGE);
      }
    }
    CommonUtil.requirePositiveInt(port, () -> PORT + " is required");
    try (Database mysql = Database.local(user, password, dbName, port)) {
      System.out.println(
          String.join(
              " ",
              Arrays.asList(
                  "user:", mysql.user(), "password:", mysql.password(), "jdbc:", mysql.url())));
      consumer.accept(mysql);
      TimeUnit.SECONDS.sleep(ttl);
    }
  }

  static void main(String[] args) throws InterruptedException {
    start(args, mysql -> {});
  }
}
