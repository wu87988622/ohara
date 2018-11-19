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

  app.get('/api/pipelines/:uuid', (req, res) => {
    axios
      .get(`${API_ROOT}/pipelines/${req.params.uuid}`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.post('/api/pipelines/create', (req, res) => {
    axios
      .post(`${API_ROOT}/pipelines`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/pipelines/update/:uuid', (req, res) => {
    axios
      .put(`${API_ROOT}/pipelines/${req.params.uuid}`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.delete('/api/pipelines/delete/:uuid', (req, res) => {
    axios
      .delete(`${API_ROOT}/pipelines/${req.params.uuid}`)
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

  app.get('/api/sources/:uuid', (req, res) => {
    axios
      .get(`${API_ROOT}/sources/${req.params.uuid}`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.post('/api/sources/create', (req, res) => {
    axios
      .post(`${API_ROOT}/sources`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/sources/update/:uuid', (req, res) => {
    axios
      .put(`${API_ROOT}/sources/${req.params.uuid}`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/sources/:uuid/start', (req, res) => {
    axios
      .put(`${API_ROOT}/sources/${req.params.uuid}/start`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/sources/:uuid/stop', (req, res) => {
    axios
      .put(`${API_ROOT}/sources/${req.params.uuid}/stop`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.post('/api/sinks/create', (req, res) => {
    axios
      .post(`${API_ROOT}/sinks`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/sinks/update/:uuid', (req, res) => {
    axios
      .put(`${API_ROOT}/sinks/${req.params.uuid}`, req.body)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.get('/api/sinks', (req, res) => {
    axios
      .get(`${API_ROOT}/sinks`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.get('/api/sinks/:uuid', (req, res) => {
    axios
      .get(`${API_ROOT}/sinks/${req.params.uuid}`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/sinks/:uuid/start', (req, res) => {
    axios
      .put(`${API_ROOT}/sinks/${req.params.uuid}/start`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  app.put('/api/sinks/:uuid/stop', (req, res) => {
    axios
      .put(`${API_ROOT}/sinks/${req.params.uuid}/stop`)
      .then(result => onSuccess(res, result))
      .catch(err => onError(res, err));
  });

  return app;
};
