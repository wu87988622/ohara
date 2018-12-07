export const CONNECTOR_KEYS = {
  jdbcSource: 'com.island.ohara.connector.jdbc.JDBCSourceConnector',
  ftpSource: 'com.island.ohara.connector.ftp.FtpSource',
  hdfsSink: 'com.island.ohara.connector.hdfs.HDFSSinkConnector',
  ftpSink: 'com.island.ohara.connector.ftp.FtpSink',
  topic: 'topic',
};

export const ICON_MAPS = {
  [CONNECTOR_KEYS.jdbcSource]: 'fa-file-import',
  [CONNECTOR_KEYS.ftpSource]: 'fa-file-import',
  [CONNECTOR_KEYS.topic]: 'fa-list-ul',
  [CONNECTOR_KEYS.hdfsSink]: 'fa-file-export',
  [CONNECTOR_KEYS.ftpSink]: 'fa-file-export',
};

export const TABLE_HEADERS = ['connector name', 'version', 'revision'];

export const CONNECTOR_FILTERS = [
  'com.island.ohara.configurator.endpoint.Validator',
  'com.island.ohara.connector.perf.PerfSource',
  'org.apache.kafka.connect.file.FileStreamSourceConnector',
  'org.apache.kafka.connect.file.FileStreamSinkConnector',
];
