import {
  isNumber,
  isDefined,
  isUuid,
  reduceByProp,
  isEmptyStr,
} from '../helpers';

describe('isEmptyStr()', () => {
  it('returns true if the given string is an empty string', () => {
    expect(isEmptyStr('')).toBe(true);
  });

  it('returns false if the given string is not an empty string', () => {
    expect(isEmptyStr('kjlf')).toBe(false);
  });
});

describe('isDefined()', () => {
  it('returns true if the given value type is defined', () => {
    expect(isDefined('')).toBe(true);
    expect(isDefined(1)).toBe(true);
    expect(isDefined(NaN)).toBe(true);
    expect(isDefined({})).toBe(true);
    expect(isDefined([])).toBe(true);
    expect(isDefined(null)).toBe(true);
    expect(isDefined(() => {})).toBe(true);
  });

  it('returns false if the given value type is undefined', () => {
    expect(isDefined(undefined)).toBe(false);
  });
});

describe('isNumber()', () => {
  it('returns true if the given value type is number', () => {
    expect(isNumber(10)).toBe(true);
  });

  it('returns false if the given value type is not number', () => {
    expect(isNumber('test me!')).toBe(false);
  });
});

describe('isUuid()', () => {
  it('returns true if the given value is a valid uuid', () => {
    const uuid = 'c0398bff-72f6-4080-985b-5a0c5feb911f';
    expect(isUuid(uuid)).toBe(true);
  });

  it('returns false if the given value is not a valid uuid', () => {
    const uuid = 'xx-xxxxx-4080-985b-xxxxxx';
    expect(isUuid(uuid)).toBe(false);
  });
});

describe('reduceByProp()', () => {
  it('returns the correct item', () => {
    const list = [
      { name: 'a', timeStamp: Date.now() },
      { name: 'b', timeStamp: Date.now() },
      { name: 'c', timeStamp: Date.now() },
    ];
    const result = reduceByProp(list, 'timeStamp');

    expect(result).toBe(list[list.length - 1]);
  });

  it('returns the item that has the biggest number', () => {
    const list = [
      { name: 'a', no: 123 },
      { name: 'b', no: 3425234423 },
      { name: 'c', no: 1221 },
    ];
    const result = reduceByProp(list, 'no');

    expect(result).toBe(list[1]);
  });
});
