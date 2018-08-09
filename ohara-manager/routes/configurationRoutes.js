// TODO: use the real APIs

module.exports = app => {
  app.post('/api/configuration/validate', (req, res) => {
    res.json({
      isSuccess: true,
      isValidate: true,
      url: req.params.url || 'no-url-specified',
    });
  });

  app.post('/api/configuration/save', (req, res) => {
    res.json({
      isSuccess: true,
      target: req.params.target || 'no-target-specified',
      url: req.params.url || 'no-url-specified',
    });
  });

  return app;
};
