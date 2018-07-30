exports.onSuccess = (res, result) => {
  result.data.status = true;
  res.json(result.data);
  return;
};

exports.onError = (res, err) => {
  if (err.response) {
    const { data: errorMessage } = err.response;
    errorMessage.status = false;

    res.json({ errorMessage });
    return;
  }
};
