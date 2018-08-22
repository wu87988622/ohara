import { get, isNull } from 'lodash';
import uuidValidate from 'uuid-validate';

const isEmptyString = val => val.length === 0;

const isEmptyArray = arr => arr.length === 0;

const isDefined = val => typeof val !== 'undefined';

const isNumber = val => typeof val === 'number';

const isUuid = val => uuidValidate(val);

export {
  get,
  isEmptyString,
  isEmptyArray,
  isDefined,
  isNumber,
  isUuid,
  isNull,
};
