const {
  getErrors,
  onSuccess,
  onValidateSuccess,
  onError,
} = require('../apiHelpers');

const MESSAGES = require('../../constants/messages');

describe('onValidateSuccess', () => {
  it('should do nothing if the given result does not contain any errors', () => {
    const res = { json: jest.fn() };
    const result = { data: [] };

    expect(onValidateSuccess(res, result)).toBe();
  });

  it('should call the res.json() with the proper params', () => {
    const res = { json: jest.fn() };
    const result = {
      data: [{ name: '1', pass: true }, { name: 'b', pass: true }],
    };

    onValidateSuccess(res, result);
    expect(res.json).toHaveBeenCalledTimes(1);
    expect(res.json).toHaveBeenCalledWith({
      hosts: result.data,
      isSuccess: true,
    });
  });

  it('should return an error message if one of the res items has pass prop set to false', () => {
    const res = { json: jest.fn() };
    const result = {
      data: [{ name: '1', pass: false }, { name: 'b', pass: true }],
    };

    onValidateSuccess(res, result);

    expect(res.json).toHaveBeenCalledTimes(1);
    expect(res.json).toHaveBeenCalledWith({
      errorMessage: {
        message: MESSAGES.VALIDATION_ERROR,
      },
      isSuccess: false,
    });
  });
});

describe('getErrors', () => {
  it('should return an empty array if the data array does not contain any errors', () => {
    const data = [{ name: 'a', pass: true }, { name: 'b', pass: true }];
    const errors = getErrors(data);
    expect(errors.length).toBe(0);
  });

  it('should not return empty array when the given data array contain non-pass items', () => {
    const data = [{ name: 'a', pass: true }, { name: 'b', pass: false }];
    const errors = getErrors(data);
    expect(errors.length).toBeGreaterThanOrEqual(1);
  });
});

describe('onSuccess', () => {
  it('should do nothing if the given result object data returns null', () => {
    const res = {};
    const result = {};

    expect(onSuccess(res, result)).toBe();
  });

  it('should call the res.json() with the proper params', () => {
    const res = { json: jest.fn() };
    const result = { data: 'some data' };

    onSuccess(res, result);

    expect(res.json).toHaveBeenCalledTimes(1);
    expect(res.json).toHaveBeenCalledWith({
      result: result.data,
      isSuccess: true,
    });
  });
});

describe('onError', () => {
  it('should do nothing if the given error object does not contain response', () => {
    const res = {};
    const err = {};

    expect(onError(res, err)).toBe();
  });

  it('should call the res.json() with the proper params', () => {
    const res = {
      json: jest.fn(),
    };

    const err = {
      response: {
        data: 'Error!',
      },
    };

    onError(res, err);

    expect(res.json).toHaveBeenCalledTimes(1);
    expect(res.json).toHaveBeenCalledWith({
      errorMessage: err.response.data,
      isSuccess: false,
    });
  });
});
