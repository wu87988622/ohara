const express = require('express');
const moxios = require('moxios');
const request = require('supertest');

const app = require('../kafkaRoutes')(express());
const { API_BASE } = require('../../constants/url');

describe('Kafka routes', () => {
  beforeEach(() => {
    moxios.install();
  });

  afterEach(() => {
    moxios.uninstall();
  });

  it('PUT /api/kafka/save', async () => {
    const apiUrl = `${API_BASE}/save`;

    moxios.stubRequest(apiUrl, {
      status: 200,
      response: {
        isSuccess: true,
      },
    });

    const res = await request(app).post('/api/kafka/save');
    expect(res.body.isSuccess).toBe(true);
    expect(res.status).toBe(200);
  });

  it('POST /api/kafka/save', async () => {
    const res = await request(app).post('/api/kafka/save');
    expect(res.body.isSuccess).toBe(true);
    expect(res.status).toBe(200);
  });
});
