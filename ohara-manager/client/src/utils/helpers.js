import get from 'lodash.get';
import isNull from 'lodash.isnull';
import isFunction from 'lodash.isfunction';
import isEmpty from 'lodash.isempty';
import isString from 'lodash.isstring';
import debounce from 'lodash.debounce';
import uuidValidate from 'uuid-validate';

const isEmptyStr = val => val.length === 0;

const isEmptyArr = arr => arr.length === 0;

const isDefined = val => typeof val !== 'undefined';

const isNumber = val => typeof val === 'number';

const isUuid = val => uuidValidate(val);

export {
  get,
  debounce,
  isEmpty,
  isString,
  isEmptyStr,
  isEmptyArr,
  isDefined,
  isNumber,
  isFunction,
  isUuid,
  isNull,
};
