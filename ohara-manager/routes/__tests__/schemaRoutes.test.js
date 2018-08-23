const express = require('express');
const moxios = require('moxios');
const request = require('supertest');
const app = require('../schemaRoutes')(express());
const { API_BASE } = require('../../constants/url');

describe.skip('sechemaRoutes', () => {
  beforeEach(() => {
    moxios.install();
  });

  afterEach(() => {
    moxios.uninstall();
  });

  test('GET /api/schemas', async () => {
    const CONFIGURATOR_GET_SCHEMA_URL = `${API_BASE}/schemas`;
    moxios.stubRequest(CONFIGURATOR_GET_SCHEMA_URL, {
      status: 200,
      response: {
        uuids: { 'b2c07622-3288-4f68-8bc5-6a921cbfb863': 'schema1' },
      },
    });
    const res = await request(app).get('/api/schemas');
    expect(res.body.isSuccess).toBe(true);
    expect(res.body.uuids['b2c07622-3288-4f68-8bc5-6a921cbfb863']).toBe(
      'schema1',
    );
    expect(moxios.requests.mostRecent().url).toBe(CONFIGURATOR_GET_SCHEMA_URL);
  });

  test('GET /api/schemas/{uuid}', async () => {
    const uuid = 'b2c07622-3288-4f68-8bc5-6a921cbfb863';
    const CONFIGURATOR_GET_SCHEMA_DETAIL_URL = `${API_BASE}/schemas/${uuid}`;
    moxios.stubRequest(CONFIGURATOR_GET_SCHEMA_DETAIL_URL, {
      status: 200,
      response: {
        disabled: 'false',
        implName: 'com.island.ohara.configurator.data.OharaSchema',
        name: 'schema1',
        orders: { a1: '1', a2: '2' },
        types: { a1: 'STRING$', a2: 'STRING$' },
        uuid: 'b2c07622-3288-4f68-8bc5-6a921cbfb863',
      },
    });
    const res = await request(app).get(`/api/schemas/${uuid}`);
    expect(res.body.isSuccess).toBe(true);
    expect(res.body.name).toBe('schema1');
    expect(res.body.disabled).toBe('false');
    expect(moxios.requests.mostRecent().url).toBe(
      CONFIGURATOR_GET_SCHEMA_DETAIL_URL,
    );
  });

  test('POST /api/schemas', async () => {
    const CONFIGURATOR_POST_CREATE_SCHEMA_URL = `${API_BASE}/schemas`;
    moxios.stubRequest(CONFIGURATOR_POST_CREATE_SCHEMA_URL, {
      status: 200,
      response: {
        uuid: 'f5df1620-d9b7-4dff-8df5-13c6f4122cd3',
      },
    });
    const res = await request(app).post('/api/schemas');
    expect(res.body.isSuccess).toBe(true);
    expect(res.body.uuid).toBe('f5df1620-d9b7-4dff-8df5-13c6f4122cd3');
    expect(moxios.requests.mostRecent().url).toBe(
      CONFIGURATOR_POST_CREATE_SCHEMA_URL,
    );
  });
});
