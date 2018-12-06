package com.island.ohara.integration;

import static com.wix.mysql.EmbeddedMysql.anEmbeddedMysql;
import static com.wix.mysql.config.Charset.UTF8;
import static com.wix.mysql.config.MysqldConfig.aMysqldConfig;
import static com.wix.mysql.distribution.Version.v5_7_latest;

import com.island.ohara.common.util.CloseOnce;
import com.island.ohara.common.util.CommonUtil;
import com.wix.mysql.EmbeddedMysql;
import com.wix.mysql.config.MysqldConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface Database extends AutoCloseable {

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

  /**
   * create an embedded mysql with specific port
   *
   * @param port bound port
   * @return an embedded mysql
   */
  static Database local(int port) {
    int count = 0;
    port = (port <= 0) ? Integration.availablePort() : port;
    MysqldConfig config =
        aMysqldConfig(v5_7_latest)
            .withCharset(UTF8)
            .withUser("user-" + (count++), "password-" + (count++))
            .withTimeZone(CommonUtil.timezone())
            .withTimeout(2, TimeUnit.MINUTES)
            .withServerVariable("max_connect_errors", 666)
            .withTempDir(Integration.createTempDir("my_sql").getAbsolutePath())
            .withPort(port)
            // make mysql use " replace '
            // see https://stackoverflow.com/questions/13884854/mysql-double-quoted-table-names
            .withServerVariable("sql-mode", "ANSI_QUOTES")
            .build();
    String _dbName = "db-" + (count++);
    EmbeddedMysql mysqld = anEmbeddedMysql(config).addSchema(_dbName).start();
    return new Database() {
      private Connection connection = null;

      @Override
      public void close() {
        CloseOnce.close(connection);
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
        return _dbName;
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
                    public void close() throws Exception {
                      if (connection != null) {
                        connection.close();
                      }
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
}
