import toastr from 'toastr';
import { GENERIC_ERROR } from '../constants/message';

export const handleError = err => {
  if (err.data && !err.data.status) {
    toastr.error(GENERIC_ERROR);
    return;
  }

  toastr.error(err);
};
