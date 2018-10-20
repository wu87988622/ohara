const yargs = require('yargs');

const _ = require('../utils/helpers');

const argv = yargs
  .options({
    configurator: {
      demandOption: true,
      describe: 'Ohara configurator api',
      string: true,
      alias: 'c',
    },
    port: {
      describe: 'Ohara manager port, defaults to 5050',
      default: 5050,
      alias: 'p',
    },
  })
  .help()
  .alias('help', 'h')
  .check(argv => {
    if (_.isEmptyStr(argv.configurator))
      throw Error('--configurator cannot be empty');

    if (!_.isNumber(argv.port)) throw Error('--port can only accept number');

    return true;
  }).argv;

exports.API_ROOT = argv.configurator;
exports.PORT = argv.port;
