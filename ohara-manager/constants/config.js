const getConfig = require('../utils/configHelpers');

const { configurator, port } = getConfig;

module.exports = {
  API_ROOT: configurator,
  PORT: port,
};
