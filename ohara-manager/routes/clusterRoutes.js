const axios = require('axios');

const { API_ROOT } = require('../constants/config');
const { onSuccess, onError } = require('../utils/apiHelpers');

module.exports = app => {
  app.get('/api/cluster', (req, res) => {
    axios
      .get(`${API_ROOT}/cluster`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });
  return app;
};
