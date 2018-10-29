const _ = require('../helpers');

describe('isEmptyStr', () => {
  it('should return turn if the given value is an empty string', () => {
    expect(_.isEmptyStr('')).toBe(true);
  });

  it('should return false if the given value is not an empty string', () => {
    expect(_.isEmptyStr('xyz')).toBe(false);
  });
});

describe('isNumber', () => {
  it('should return true if the given value is a number', () => {
    expect(_.isNumber(123)).toBe(true);
  });

  it('should return false if the given value is not a number', () => {
    expect(_.isNumber([])).toBe(false);
    expect(_.isNumber({})).toBe(false);
    expect(_.isNumber(null)).toBe(false);
    expect(_.isNumber(() => {})).toBe(false);
    expect(_.isNumber('test')).toBe(false);
    expect(_.isNumber(false)).toBe(false);
  });
});
