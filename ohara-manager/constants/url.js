const configurator_api =
  process.env.CONFIGURATOR_API || 'http://localhost:9999/v0';
console.log(configurator_api)
exports.API_BASE = configurator_api;
