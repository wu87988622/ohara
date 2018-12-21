const axios = require('axios');

const { API_ROOT } = require('../config');
const {
  onSuccess,
  onError,
  onValidateSuccess,
} = require('../utils/apiHelpers');

module.exports = app => {
  app.get('/api/pipelines', (req, res) => {
    axios
      .get(`${API_ROOT}/pipelines`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.get('/api/pipelines/:id', (req, res) => {
    axios
      .get(`${API_ROOT}/pipelines/${req.params.id}`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.post('/api/pipelines/create', (req, res) => {
    axios
      .post(`${API_ROOT}/pipelines`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/pipelines/update/:id', (req, res) => {
    axios
      .put(`${API_ROOT}/pipelines/${req.params.id}`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.delete('/api/pipelines/delete/:id', (req, res) => {
    axios
      .delete(`${API_ROOT}/pipelines/${req.params.id}`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/pipelines/validate/rdb', (req, res) => {
    axios
      .put(`${API_ROOT}/validate/rdb`, req.body)
      .then(result => onValidateSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/pipelines/validate/ftp', (req, res) => {
    axios
      .put(`${API_ROOT}/validate/ftp`, req.body)
      .then(result => onValidateSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.post('/api/pipelines/query/rdb', (req, res) => {
    axios
      .post(`${API_ROOT}/query/rdb`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/pipelines/validate/ftp', (req, res) => {
    axios
      .put(`${API_ROOT}/validate/ftp`, req.body)
      .then(result => onValidateSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.get('/api/sources/:id', (req, res) => {
    axios
      .get(`${API_ROOT}/sources/${req.params.id}`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.post('/api/sources/create', (req, res) => {
    axios
      .post(`${API_ROOT}/sources`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/sources/update/:id', (req, res) => {
    axios
      .put(`${API_ROOT}/sources/${req.params.id}`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/sources/:id/start', (req, res) => {
    axios
      .put(`${API_ROOT}/sources/${req.params.id}/start`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/sources/:id/stop', (req, res) => {
    axios
      .put(`${API_ROOT}/sources/${req.params.id}/stop`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.post('/api/sinks/create', (req, res) => {
    axios
      .post(`${API_ROOT}/sinks`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/sinks/update/:id', (req, res) => {
    axios
      .put(`${API_ROOT}/sinks/${req.params.id}`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.get('/api/sinks', (req, res) => {
    axios
      .get(`${API_ROOT}/sinks`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.get('/api/sinks/:id', (req, res) => {
    axios
      .get(`${API_ROOT}/sinks/${req.params.id}`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/sinks/:id/start', (req, res) => {
    axios
      .put(`${API_ROOT}/sinks/${req.params.id}/start`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/sinks/:id/stop', (req, res) => {
    axios
      .put(`${API_ROOT}/sinks/${req.params.id}/stop`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  return app;
};
