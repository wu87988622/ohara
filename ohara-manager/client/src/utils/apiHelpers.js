import toastr from 'toastr';
import { get } from './helpers';

export const handleError = err => {
  const message = get(err, 'data.errorMessage.message');

  if (message) {
    toastr.error(message);
    return;
  }

  toastr.error(err);
};
