export const ICON_KEYS = {
  jdbcSource: 'com.island.ohara.connector.jdbc.JDBCSourceConnector',
  ftpSource: 'com.island.ohara.connector.ftp.FtpSource',
  hdfsSink: 'com.island.ohara.connector.hdfs.HDFSSinkConnector',
};

export const ICON_MAPS = {
  [ICON_KEYS.jdbcSource]: 'fa-database',
  [ICON_KEYS.ftpSource]: 'fa-upload',
  topic: 'fa-list-ul',
  [ICON_KEYS.hdfsSink]: 'icon-hadoop',
};
