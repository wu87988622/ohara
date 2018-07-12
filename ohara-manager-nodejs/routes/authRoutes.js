module.exports = app => {
  app.post('/api/login', (req, res) => {
    const { username, password } = req.body;

    if (username && password) {
      const data = {
        id: 12121,
        username,
        auth_token:
          'okaythisisasupersecrettokenthatyouwillneverbeabletodecondeiguess',
      };

      res.status(200).json(data);
      return;
    }

    res.status(400).send('You should provide both username and password');
  });

  app.get('/api/current_user', (req, res) => {
    const currentUser = {
      name: 'Joshua',
      age: 20,
      gender: 'male',
      occupation: 'Frontend dev',
      location: 'Taiwan',
    };

    res.status(200).send(currentUser);
  });
};
