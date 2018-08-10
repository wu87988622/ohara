// TODO: use the real APIs

const axios = require('axios');

const { API_BASE } = require('../constants/url');
const { onSuccess, onValideSuccess, onError } = require('../utils/apiHelpers');

module.exports = app => {
  app.get('/api/configuration/hdfs', (req, res) => {
    axios
      .get(`${API_BASE}/hdfs`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/configuration/validate/hdfs', (req, res) => {
    axios
      .put(`${API_BASE}/validate/hdfs`, req.body)
      .then(result => onValideSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.post('/api/configuration/save/hdfs', (req, res) => {
    axios
      .post(`${API_BASE}/hdfs`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  return app;
};
