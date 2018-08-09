// TODO: use the real APIs
const axios = require('axios');

const { API_BASE } = require('../constants/url');
const { onSuccess, onError } = require('../utils/apiHelpers');

module.exports = app => {
  app.put('/api/kafka/validate', (req, res) => {
    axios
      .put(`${API_BASE}/validate`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.post('/api/kafka/save', (req, res) => {
    res.json({
      isSuccess: true,
    });
  });
  return app;
};
