export const isEmptyString = val => val.length === 0;

export const isEmptyArray = arr => arr.length === 0;

export const isDefined = val => typeof val !== 'undefined';

export const isNumber = val => typeof val === 'number';

export const double = list => {
  const isArray = Array.isArray(list);
  const _isNumber = isArray && list.every(item => isNumber(item));

  const isValidInput = isArray && _isNumber;

  if (isValidInput) {
    const result = list.map(item => {
      return item * 2;
    });
    return result;
  }

  return null;
};
