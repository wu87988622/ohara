package com.island.ohara.integration;

import static com.wix.mysql.EmbeddedMysql.anEmbeddedMysql;
import static com.wix.mysql.config.Charset.UTF8;
import static com.wix.mysql.config.MysqldConfig.aMysqldConfig;
import static com.wix.mysql.distribution.Version.v5_7_latest;

import com.island.ohara.common.util.CommonUtil;
import com.wix.mysql.EmbeddedMysql;
import com.wix.mysql.config.MysqldConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public interface Database extends AutoCloseable {

  String DB_SERVER = "ohara.it.db";
  AtomicInteger COUNT = new AtomicInteger(0);

  String hostname();

  int port();

  String databaseName();

  String user();

  String password();

  String url();

  Connection connection();

  final class ConnectionInfo {
    private static String dbInstance;
    private static String user;
    private static String password;
    private static String host;
    private static int port;
    private static String dbName;

    public ConnectionInfo(
        String dbInstance, String user, String password, String host, Integer port, String dbName) {
      this.dbInstance = dbInstance;
      this.user = user;
      this.password = password;
      this.host = host;
      this.port = port;
      this.dbName = dbName;
    }

    public String getDbInstance() {
      return dbInstance;
    }

    public String getUser() {
      return user;
    }

    public String getPassword() {
      return password;
    }

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }

    public String getDbName() {
      return dbName;
    }
  }

  /** @return true if this database is generated locally. */
  public Boolean isLocal();

  static ConnectionInfo parseString(String dbString) {
    // format => jdbc:db:user:password@//host:port/db_name
    if (!dbString.startsWith("jdbc")) throw new IllegalArgumentException("invalid url:" + dbString);
    try {
      String dbInstance = dbString.split(":")[1];
      String user = dbString.split(":")[2];
      String password = dbString.split(":")[3].split("@//")[0];
      String host = dbString.split(":")[3].split("@//")[1];
      Integer port = Integer.parseInt(dbString.split(":")[4].split("/")[0]);
      String dbName = dbString.split(":")[4].split("/")[1];
      return new ConnectionInfo(dbInstance, user, password, host, port, dbName);
    } catch (Exception e) {
      throw new IllegalArgumentException("invalid value from " + DB_SERVER, e);
    }
  }

  /**
   * create an embedded mysql with specific port
   *
   * @param port bound port
   * @return an embedded mysql
   */
  static Database local(int port) {
    port = (port <= 0) ? Integration.availablePort() : port;
    MysqldConfig config =
        aMysqldConfig(v5_7_latest)
            .withCharset(UTF8)
            .withUser("user-" + COUNT.getAndIncrement(), "password-" + COUNT.getAndIncrement())
            .withTimeZone(CommonUtil.timezone())
            .withTimeout(2, TimeUnit.MINUTES)
            .withServerVariable("max_connect_errors", 666)
            .withTempDir(Integration.createTempDir("my_sql").getAbsolutePath())
            .withPort(port)
            // make mysql use " replace '
            // see https://stackoverflow.com/questions/13884854/mysql-double-quoted-table-names
            .withServerVariable("sql-mode", "ANSI_QUOTES")
            .build();
    String _dbName = "db-" + COUNT.getAndIncrement();
    EmbeddedMysql mysqld = anEmbeddedMysql(config).addSchema(_dbName).start();
    return new Database() {
      private Connection connection = null;

      @Override
      public void close() throws Exception {
        if (connection != null) {
          connection.close();
        }
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
      public Boolean isLocal() {
        return true;
      }
    };
  }

  static Database of() {
    return of(System.getenv(DB_SERVER));
  }

  static Database of(String db) {
    return Optional.ofNullable(db)
        .map(
            d -> {
              ConnectionInfo connectionInfo = parseString(d);
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
                      return connectionInfo.getHost();
                    }

                    @Override
                    public int port() {
                      return connectionInfo.getPort();
                    }

                    @Override
                    public String databaseName() {
                      return connectionInfo.getDbName();
                    }

                    @Override
                    public String user() {
                      return connectionInfo.getUser();
                    }

                    @Override
                    public String password() {
                      return connectionInfo.getPassword();
                    }

                    @Override
                    public String url() {
                      return "jdbc:"
                          + connectionInfo.getDbInstance()
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
                    public Boolean isLocal() {
                      return false;
                    }
                  };
            })
        .orElseGet(() -> local(0));
  }
}
