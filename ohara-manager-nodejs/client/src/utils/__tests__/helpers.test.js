import { isNumber, isDefined } from '../helpers';

describe('isDefined', () => {
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

describe('isNumber', () => {
  it('returns true if the given value type is number', () => {
    expect(isNumber(10)).toBe(true);
  });

  it('returns false if the given value type is not number', () => {
    expect(isNumber('test me!')).toBe(false);
  });
});
