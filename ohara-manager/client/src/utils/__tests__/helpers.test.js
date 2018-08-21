import { isNumber, isDefined, isUuid } from '../helpers';

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

describe('isUuid', () => {
  it('returns true if the given value is a valid uuid', () => {
    const uuid = 'c0398bff-72f6-4080-985b-5a0c5feb911f';
    expect(isUuid(uuid)).toBe(true);
  });

  it('returns false if the given value is not a valid uuid', () => {
    const uuid = 'xx-xxxxx-4080-985b-xxxxxx';
    expect(isUuid(uuid)).toBe(false);
  });
});
