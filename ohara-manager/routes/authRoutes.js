const { fakeUsers, token } = require('../constants/fakeUsers');

// TODO: fake routes, this is due to we haven't implement the real auth in our app
module.exports = app => {
  app.post('/api/login', (req, res) => {
    const { username, password } = req.body;

    const user = fakeUsers.find(
      ({ email, pass }) =>
        email === username.toLowerCase() && pass === password,
    );

    if (user) {
      return res.status(200).json({
        isSuccess: true,
        name: user.name,
        avatar: user.avatar,
        loggedinOn: Date.now(),
        token,
      });
    }

    res.status(200).json({
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
