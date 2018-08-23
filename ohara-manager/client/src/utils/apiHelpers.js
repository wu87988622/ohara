import toastr from 'toastr';
import * as _ from './helpers';

export const handleError = err => {
  const message = _.get(err, 'data.errorMessage.message');

  if (message) {
    toastr.error(message);
    return;
  }

  toastr.error(err);
};
