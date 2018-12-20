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
// gzip static assets
app.use(compression());

app.use(morgan('combined'));

app.use(express.static(path.resolve(__dirname, 'client', 'build')));

// API routes
require('./routes/authRoutes')(app);
require('./routes/topicRoutes')(app);
require('./routes/schemaRoutes')(app);
require('./routes/configurationRoutes')(app);
require('./routes/clusterRoutes')(app);
require('./routes/pipelineRoutes')(app);

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
        // incase if content-type is application/x-www-form-urlencoded -> we need to change to application/json
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
  console.log(chalk.blue(`Configurator API: ${API_ROOT}`));
});
