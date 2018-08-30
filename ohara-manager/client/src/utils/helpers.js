import get from 'lodash.get';
import isNull from 'lodash.isnull';
import isFunction from 'lodash.isfunction';
import uuidValidate from 'uuid-validate';

const isEmptyStr = val => val.length === 0;

const isEmptyArr = arr => arr.length === 0;

const isDefined = val => typeof val !== 'undefined';

const isNumber = val => typeof val === 'number';

const isUuid = val => uuidValidate(val);

export {
  get,
  isEmptyStr,
  isEmptyArr,
  isDefined,
  isNumber,
  isFunction,
  isUuid,
  isNull,
};
