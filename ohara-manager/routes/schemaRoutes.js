const axios = require('axios');

const { API_ROOT } = require('../constants/url');
const { onSuccess, onError } = require('../utils/apiHelpers');

module.exports = app => {
  app.post('/api/schemas', (req, res) => {
    axios
      .post(`${API_ROOT}/schemas`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.get('/api/schemas', (req, res) => {
    axios
      .get(`${API_ROOT}/schemas`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.get('/api/schemas/:uuid', (req, res) => {
    const uuid = req.params.uuid;
    axios
      .get(`${API_ROOT}/schemas/${uuid}`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  return app;
};
