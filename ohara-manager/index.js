const express = require('express');
const bodyParser = require('body-parser');
const compression = require('compression');
const chalk = require('chalk');
const path = require('path');
const morgan = require('morgan');

/* eslint-disable no-console */

const OHARA_MANAGER_PORT = process.env.OHARA_MANAGER_PORT || 5050;
const API_ROOT = process.env.CONFIGURATOR_API;
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

app.get('*', (req, res) => {
  res.sendFile(path.resolve(__dirname, 'client', 'build', 'index.html'));
});

app.listen(OHARA_MANAGER_PORT, () => {
  console.log(chalk.green(`Ohara manager is running at port: ${OHARA_MANAGER_PORT}`));

  if (!API_ROOT) {
    console.log(chalk.red(`CONFIGURATOR_API_ROOT did not specify!`));
  } else {
    console.log(
      chalk.blue(`CONFIGURATOR_API_ROOT: ${process.env.CONFIGURATOR_API}`),
    );
  }
});
