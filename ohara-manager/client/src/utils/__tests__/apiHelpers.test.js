import toastr from 'toastr';
import { handleError } from '../apiHelpers';

jest.mock('toastr', () => {
  return {
    error: jest.fn(),
  };
});

describe('handleError', () => {
  it('calls toastr.error()', () => {
    expect(toastr.error).toHaveBeenCalledTimes(0);

    handleError('throw an error');
    expect(toastr.error).toHaveBeenCalledTimes(1);
  });
});
