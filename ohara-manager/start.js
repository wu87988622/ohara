/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const express = require('express');
const bodyParser = require('body-parser');
const compression = require('compression');
const chalk = require('chalk');
const path = require('path');
const morgan = require('morgan');
const proxy = require('http-proxy-middleware');

const { API_ROOT, PORT } = require('./config');

/* eslint-disable no-console */
const app = express();

app.use(bodyParser.json());
// Gzip static assets
app.use(compression());

// Server logs
app.use(morgan('combined'));

// Serve client build dir
app.use(express.static(path.resolve(__dirname, 'client', 'build')));

// API Proxy
app.use(
  '/api',
  proxy({
    target: API_ROOT,
    changeOrigin: true,
    pathRewrite: {
      '/api': '',
    },
    onProxyReq: (proxyReq, req) => {
      const contentType = req.headers['content-type'];
      if (contentType && contentType.indexOf('multipart/form-data') === 0) {
        return;
      }

      if (req.body) {
        const bodyData = JSON.stringify(req.body);
        // in case if content-type is application/x-www-form-urlencoded -> we need to change to application/json
        proxyReq.setHeader('Content-Type', 'application/json');
        proxyReq.setHeader('Content-Length', Buffer.byteLength(bodyData));
        proxyReq.write(bodyData);
      }
    },
  }),
);

app.get('*', (req, res) => {
  res.sendFile(path.resolve(__dirname, 'client', 'build', 'index.html'));
});

app.listen(PORT, () => {
  console.log(chalk.green(`Ohara manager is running at port: ${PORT}`));
});
