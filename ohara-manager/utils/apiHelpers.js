const _ = require('lodash');

const getErrors = data => {
  const errors = data.reduce((acc, r) => {
    if (!r.pass) acc.push(r);
    return acc;
  }, []);

  return errors;
};

exports.onValideSuccess = (res, result) => {
  const errors = getErrors(result.data);

  if (errors.length > 0) {
    return res.json({
      isSuccess: false,
      errorMessage: { message: 'Failing to passed!' },
    });
  }

  const _res = {
    isSuccess: true,
    hosts: result.data,
  };

  return res.json(_res);
};

exports.onSuccess = (res, result) => {
  const data = _.get(result, 'data', null);

  if (data) {
    const _result = {
      result: data,
      isSuccess: true,
    };
    return res.json(_result);
  }
};

exports.onError = (res, err) => {
  if (err.response) {
    const { data: errorMessage } = err.response;
    res.json({ errorMessage, isSuccess: false });
    return;
  }
};
