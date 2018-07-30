module.exports = app => {
  app.post('/api/login', (req, res) => {
    const { username, password } = req.body;
    const isValidUser =
      username.toLowerCase() === 'joshua' && password === '111111';

    if (isValidUser) {
      res.status(200).json({
        isSuccess: true,
        token: 'sdfjlsjdfksjdf',
      });

      return;
    }

    res.status(400).json({
      isSuccess: false,
      errMsg: 'Invalid username or password',
    });
  });

  app.get('/api/logout', (req, res) => {
    res.status(200).json({
      isSuccess: true,
    });
  });

  return app;
};
