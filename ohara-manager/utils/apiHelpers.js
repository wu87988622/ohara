exports.onSuccess = (res, result) => {
  result.data.isSuccess = true;
  res.json(result.data);
  return;
};

exports.onError = (res, err) => {
  if (err.response) {
    const { data: errorMessage } = err.response;
    res.json({ errorMessage, isSuccess: false });
    return;
  }
};
