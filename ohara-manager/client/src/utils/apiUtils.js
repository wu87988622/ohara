import toastr from 'toastr';

import * as _ from './commonUtils';

export const handleError = err => {
  const message = _.get(err, 'data.errorMessage.message');

  if (message) {
    toastr.error(message);
    return;
  }

  toastr.error(err);
};

export const getErrors = data => {
  const errors = data.reduce((acc, r) => {
    if (!r.pass) acc.push(r);
    return acc;
  }, []);

  return errors;
};
