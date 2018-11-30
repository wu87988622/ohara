const yargs = require('yargs');

const _ = require('./helpers');
const getProjectVersion = require('./getProjectVersion');

const getConfig = yargs
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
  .version(getProjectVersion())
  .alias('version', 'v')
  .check(argv => {
    if (_.isEmptyStr(argv.configurator))
      throw Error('--configurator cannot be empty');

    if (!_.isNumber(argv.port)) throw Error('--port can only accept number');

    return true;
  }).argv;

module.exports = getConfig;
