// TODO: use the real APIs

const axios = require('axios');

const { API_ROOT } = require('../config');
const {
  onSuccess,
  onValidateSuccess,
  onError,
} = require('../utils/apiHelpers');

module.exports = app => {
  app.get('/api/configuration/hdfs', (req, res) => {
    axios
      .get(`${API_ROOT}/hdfs`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/configuration/validate/hdfs', (req, res) => {
    axios
      .put(`${API_ROOT}/validate/hdfs`, req.body)
      .then(result => onValidateSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.post('/api/configuration/hdfs/save', (req, res) => {
    axios
      .post(`${API_ROOT}/hdfs`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  return app;
};
