import toastr from 'toastr';
import { handleError } from '../apiUtils';

jest.mock('toastr', () => {
  return {
    error: jest.fn(),
  };
});

describe('handleError()', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('calls toastr.error() with the given error message', () => {
    expect(toastr.error).toHaveBeenCalledTimes(0);
    const err = 'error';

    handleError(err);
    expect(toastr.error).toHaveBeenCalledTimes(1);
    expect(toastr.error).toHaveBeenCalledWith(err);
  });

  it('calls toastr.error() with the given error object', () => {
    expect(toastr.error).toHaveBeenCalledTimes(0);

    const err = {
      errorMessage: { message: 'error' },
    };

    handleError(err);
    expect(toastr.error).toHaveBeenCalledTimes(1);
    expect(toastr.error).toHaveBeenCalledWith(err.errorMessage.message);
  });
});
