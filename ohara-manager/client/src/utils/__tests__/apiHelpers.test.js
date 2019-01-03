import toastr from 'toastr';
import { handleError } from '../apiUtils';

describe('handleError()', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('calls toastr.error() with the given error message', () => {
    const err = 'error';

    handleError(err);
    expect(toastr.error).toHaveBeenCalledTimes(1);
    expect(toastr.error).toHaveBeenCalledWith(err);
  });

  it('calls toastr.error() with the given error object', () => {
    const err = {
      errorMessage: { message: 'error' },
    };

    handleError(err);
    expect(toastr.error).toHaveBeenCalledTimes(1);
    expect(toastr.error).toHaveBeenCalledWith(err);
  });
});
