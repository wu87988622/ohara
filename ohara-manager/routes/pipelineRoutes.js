const axios = require('axios');

const { API_ROOT } = require('../constants/url');
const { onSuccess, onError } = require('../utils/apiHelpers');

module.exports = app => {
  app.get('/api/pipelines', (req, res) => {
    axios
      .get(`${API_ROOT}/pipelines`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.post('/api/pipelines/save', (req, res) => {
    axios
      .post(`${API_ROOT}/pipelines`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  return app;
};
