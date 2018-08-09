const express = require('express');
const request = require('supertest');
const bodyParser = require('body-parser');

const expr = express();
expr.use(bodyParser.json());
expr.use(bodyParser.urlencoded({ extended: true }));
const app = require('../authRoutes')(expr);

describe('Test auth restful api', () => {
  it('GET /api/logout', async () => {
    const res = await request(app).get('/api/logout');
    expect(res.body.isSuccess).toBe(true);
    expect(res.status).toBe(200);
  });

  it('POST /api/login', async () => {
    const res = await request(app)
      .post('/api/login')
      .send({ username: 'joshua', password: '111111' });

    expect(res.body.isSuccess).toBe(true);
    expect(res.body.token).toBe('sdfjlsjdfksjdf');
    expect(res.status).toBe(200);
  });
});
