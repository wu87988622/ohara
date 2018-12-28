import toastr from 'toastr';

import * as _ from './commonUtils';

export const handleError = err => {
  const message = _.get(err, 'errorMessage.message');

  if (message) {
    toastr.error(message);
    return;
  }

  toastr.error(err);
};
