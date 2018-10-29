const axios = require('axios');

const { API_ROOT } = require('../constants/config');
const { onSuccess, onError } = require('../utils/apiHelpers');

module.exports = app => {
  app.post('/api/topics', (req, res) => {
    axios
      .post(`${API_ROOT}/topics`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.get('/api/topics', (req, res) => {
    axios
      .get(`${API_ROOT}/topics`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.get('/api/topics/:uuid', (req, res) => {
    axios
      .get(`${API_ROOT}/topics/${req.params.uuid}`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  return app;
};
