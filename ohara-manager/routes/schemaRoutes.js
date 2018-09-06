module.exports = app => {
  // TODO: Replace these temp fake data that are feeding to Frontend with the real API
  app.get('/api/schemas', (req, res) => {
    res.json([
      { sn: '1', name: 'col_1', type: 'INT' },
      { sn: '2', name: 'col_2', type: 'STRING' },
      { sn: '3', name: 'col_3', type: 'DATE' },
      { sn: '4', name: 'col_4', type: 'STRING' },
      { sn: '5', name: 'col_5', type: 'INT' },
      { sn: '6', name: 'col_6', type: 'BOOLEAN' },
    ]);
  });
  return app;
};
