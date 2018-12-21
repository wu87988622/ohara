const axios = require('axios');

const { API_ROOT } = require('../config');
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

  app.get('/api/topics/:id', (req, res) => {
    axios
      .get(`${API_ROOT}/topics/${req.params.id}`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  return app;
};
