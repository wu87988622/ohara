package com.island.ohara.integration;

import com.island.ohara.common.util.CommonUtil;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
 * java.io.tmpdir with prefix - ftp 1) port -> a random port 2) host -> "localhost" 3) user -> a
 * writable account 4) password -> a writable account
 *
 * <p>all resources will be released by FtpServer#close(). For example, all data in home folder will
 * be deleted
 *
 * <p>If ohara.it.ftp exists in env variables, local ftp server is not created.
 */
public interface FtpServer extends AutoCloseable {
  String FTP_SERVER = "ohara.it.ftp";

  int NUMBER_OF_FTPSERVER = 3;

  AtomicInteger COUNT = new AtomicInteger(0);

  String host();

  Integer port();

  String user();

  String password();

  /**
   * If the ftp server is in passive mode, the port is used to transfer data
   *
   * @return data port
   */
  List<Integer> dataPort();

  /** @return true if this ftp server is generated locally. */
  boolean isLocal();

  final class FtpServerInfo {
    private static String user;
    private static String password;
    private static String host;
    private static Integer port;

    public FtpServerInfo(String user, String password, String host, Integer port) {
      this.user = user;
      this.password = password;
      this.host = host;
      this.port = port;
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

    public Integer getPort() {
      return port;
    }
  }

  static FtpServerInfo parseString(String ftpString) {
    // format => user:password@host:port
    try {
      String user = ftpString.split(":")[0];
      String password = ftpString.split("@")[0].split(":")[1];
      String host = ftpString.split("@")[1].split(":")[0];
      Integer port = Integer.parseInt(ftpString.split("@")[1].split(":")[1]);
      return new FtpServerInfo(user, password, host, port);
    } catch (Exception e) {
      throw new IllegalArgumentException("invalid value from " + FTP_SERVER, e);
    }
  }

  /**
   * create an embedded ftp server with specific port
   *
   * @param commandPort bound port used to control
   * @param dataPorts bound port used to transfer data
   * @return an embedded ftp server
   */
  static FtpServer local(Integer commandPort, int[] dataPorts) {
    File homeFolder = Integration.createTempDir("ftp");
    PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
    UserManager userManager = userManagerFactory.createUserManager();
    BaseUser _user = new BaseUser();
    _user.setName("user-" + COUNT.getAndIncrement());
    _user.setAuthorities(Arrays.asList(new WritePermission()));
    _user.setEnabled(true);
    _user.setPassword("password-" + COUNT.getAndIncrement());
    _user.setHomeDirectory(homeFolder.getAbsolutePath());
    try {
      userManager.save(_user);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    ListenerFactory listenerFactory = new ListenerFactory();
    listenerFactory.setPort(commandPort);
    DataConnectionConfigurationFactory connectionConfig = new DataConnectionConfigurationFactory();
    List<Integer> portList = new ArrayList<Integer>();
    for (Integer dataPort : dataPorts) {
      portList.add(dataPort);
    }
    List<Integer> availableDataPorts =
        portList
            .stream()
            .map(
                port -> {
                  return (port <= 0) ? Integration.availablePort() : port;
                })
            .collect(Collectors.toList());

    connectionConfig.setActiveEnabled(false);
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
      public void close() throws Exception {
        server.stop();
        Integration.deleteFiles(homeFolder);
      }

      @Override
      public String host() {
        return CommonUtil.hostname();
      }

      @Override
      public Integer port() {
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
      public List<Integer> dataPort() {
        return availableDataPorts;
      }

      @Override
      public boolean isLocal() {
        return true;
      }
    };
  }

  static FtpServer of() {
    return of(System.getenv(FTP_SERVER));
  }

  static FtpServer of(String ftpServer) {
    return Optional.ofNullable(ftpServer)
        .map(
            f -> {
              FtpServerInfo ftpServerInfo = parseString(f);
              return (FtpServer)
                  new FtpServer() {
                    @Override
                    public void close() throws Exception {
                      // Nothing
                    }

                    @Override
                    public String host() {
                      return ftpServerInfo.getHost();
                    }

                    @Override
                    public Integer port() {
                      return ftpServerInfo.getPort();
                    }

                    @Override
                    public String user() {
                      return ftpServerInfo.getUser();
                    }

                    @Override
                    public String password() {
                      return ftpServerInfo.getPassword();
                    }

                    @Override
                    public List<Integer> dataPort() {
                      throw new UnsupportedOperationException(
                          "TODO: can't get data port from actual ftp server");
                    }

                    @Override
                    public boolean isLocal() {
                      return false;
                    }
                  };
            })
        .orElseGet(() -> local(0, IntStream.range(1, NUMBER_OF_FTPSERVER).map(x -> 0).toArray()));
  }

  static String mkPortString(List<Integer> ports) {
    return ports.stream().map(p -> String.valueOf(p)).collect(Collectors.joining(","));
  }
}
