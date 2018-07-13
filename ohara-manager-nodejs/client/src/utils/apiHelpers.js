import toastr from 'toastr';

export const handleError = err => {
  toastr.error(err);
};
