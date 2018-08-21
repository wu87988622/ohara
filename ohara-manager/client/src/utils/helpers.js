import { get, isNull } from 'lodash';
import uuidValidate from 'uuid-validate';

export const isEmptyString = val => val.length === 0;

export const isEmptyArray = arr => arr.length === 0;

export const isDefined = val => typeof val !== 'undefined';

export const isNumber = val => typeof val === 'number';

export const isUuid = val => uuidValidate(val);

export { get, isNull };
