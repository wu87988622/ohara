const express = require('express');
const request = require('supertest');
const bodyParser = require('body-parser');
const { fakeUsers, token } = require('../../constants/fakeUsers');

const expr = express();
expr.use(bodyParser.json());
expr.use(bodyParser.urlencoded({ extended: true }));
const app = require('../authRoutes')(expr);

describe('authRoutes', () => {
  it('GET /api/logout', async () => {
    const res = await request(app).get('/api/logout');
    expect(res.body.isSuccess).toBe(true);
    expect(res.status).toBe(200);
  });

  it('POST /api/login', async () => {
    const res = await request(app)
      .post('/api/login')
      .send({ username: fakeUsers[0].email, password: fakeUsers[0].pass });

    expect(res.body.isSuccess).toBe(true);
    expect(res.body.name).toBe(fakeUsers[0].name);
    expect(res.body.avatar).toBe(fakeUsers[0].avatar);
    expect(res.body.token).toBe(token);
    expect(res.status).toBe(200);
  });
});
