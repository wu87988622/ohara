import toastr from 'toastr';

export const handleError = err => {
  if (err.data && !err.data.isSuccess) {
    toastr.error(err.data.errMsg);
    return;
  }

  toastr.error(err);
};
