const _ = require('../utils/helpers');
const MESSAGES = require('../constants/messages');

const getErrors = data => {
  const errors = data.reduce((acc, r) => {
    if (!r.pass) acc.push(r);
    return acc;
  }, []);

  return errors;
};

const onValidateSuccess = (res, result) => {
  const errors = getErrors(result.data);

  if (errors.length > 0) {
    return res.json({
      isSuccess: false,
      errorMessage: {
        message: MESSAGES.VALIDATION_ERROR,
      },
    });
  }

  const _res = {
    isSuccess: true,
    hosts: result.data,
  };

  return res.json(_res);
};

const onSuccess = (res, result) => {
  const data = _.get(result, 'data', null);

  if (data) {
    const _result = {
      result: data,
      isSuccess: true,
    };
    return res.json(_result);
  }
};

const onError = (res, err) => {
  if (err.response) {
    const { data: errorMessage } = err.response;
    res.json({ errorMessage, isSuccess: false });
    return;
  }
};

module.exports = {
  onValidateSuccess,
  onSuccess,
  onError,
  getErrors, // for unit tests
};
