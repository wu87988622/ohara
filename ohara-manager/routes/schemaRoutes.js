const axios = require('axios');

const { API_BASE } = require('../constants/url');
const { onSuccess, onError } = require('../utils/apiHelpers');

module.exports = app => {
  app.post('/api/schemas', (req, res) => {
    axios
      .post(`${API_BASE}/schemas`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.get('/api/schemas', (req, res) => {
    axios
      .get(`${API_BASE}/schemas`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.get('/api/schemas/:uuid', (req, res) => {
    const uuid = req.params.uuid;
    axios
      .get(`${API_BASE}/schemas/${uuid}`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  return app;
};
