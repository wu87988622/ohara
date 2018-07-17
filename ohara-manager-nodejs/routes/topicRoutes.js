const axios = require('axios');

const { API_BASE } = require('../constants/url');
const { onSuccess, onError } = require('../utils/apiHelpers');

module.exports = app => {
  app.post('/api/topics', (req, res) => {
    axios
      .post(`${API_BASE}/topics`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.get('/api/topics', (req, res) => {
    axios
      .get(`${API_BASE}/topics`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });
};
