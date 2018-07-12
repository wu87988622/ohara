import { isNumber, double } from '../helpers';

describe('isNumber', () => {
  it('returns true if the given value type is number', () => {
    expect(isNumber(10)).toBe(true);
  });

  it('returns false if the given value type is not number', () => {
    expect(isNumber('test me!')).toBe(false);
  });
});

describe('double', () => {
  it('doubles the given input', () => {
    expect(double([2, 4])).toEqual([4, 8]);
  });

  it('return null when the given input has wrong type of items', () => {
    expect(double([2, 'string'])).toBe(null);
  });

  it('return null when the given input is not an array', () => {
    expect(double({})).toBe(null);
  });
});
