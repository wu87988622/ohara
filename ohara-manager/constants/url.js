const configurator_api =
  process.env.CONFIGURATOR_API || 'http://localhost:9999/v0';
exports.API_ROOT = configurator_api;
const ohara_manager_port = process.env.OHARA_MANAGER_PORT || 5050;
exports.OHARA_MANAGER_PORT = ohara_manager_port;
