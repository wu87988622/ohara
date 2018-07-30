const express = require('express');
const moxios = require('moxios');
const request = require('supertest');
const app = require('../topicRoutes')(express());
const { API_BASE } = require('../../constants/url');

describe('Test topic restful api', () => {
  beforeEach(() => {
    moxios.install();
  });

  afterEach(() => {
    moxios.uninstall();
  });

  it('GET /api/topics', async () => {
    const CONFIGURATOR_GET_TOPIC_URL = `${API_BASE}/topics`;
    moxios.stubRequest(CONFIGURATOR_GET_TOPIC_URL, {
      status: 200,
      response: {
        uuids: { 'efee24c0-6c64-40f3-ad80-487ff86b04fa': 'topic1' },
      },
    });
    const res = await request(app).get('/api/topics');
    expect(res.body.status).toBe(true);
    expect(res.body.uuids['efee24c0-6c64-40f3-ad80-487ff86b04fa']).toBe(
      'topic1',
    );
    expect(moxios.requests.mostRecent().url).toBe(CONFIGURATOR_GET_TOPIC_URL);
  });

  it('POST /api/topics', async () => {
    const CONFIGURATOR_POST_CREATE_TOPIC_URL = `${API_BASE}/topics`;
    moxios.stubRequest(CONFIGURATOR_POST_CREATE_TOPIC_URL, {
      status: 200,
      response: {
        uuid: '0abb8352-a6ca-4c8f-a3a0-f3dd6ab9fbe0',
      },
    });
    const res = await request(app).post('/api/topics');
    expect(res.body.status).toBe(true);
    expect(res.body.uuid).toBe('0abb8352-a6ca-4c8f-a3a0-f3dd6ab9fbe0');
    expect(moxios.requests.mostRecent().url).toBe(
      CONFIGURATOR_POST_CREATE_TOPIC_URL,
    );
  });
});
