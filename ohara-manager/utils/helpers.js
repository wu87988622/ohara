const _ = require('lodash');

const isEmptyStr = val => val.length === 0;

module.exports = {
  isNumber: _.isNumber,
  isEmptyStr,
};
