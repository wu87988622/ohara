const express = require('express');
const bodyParser = require('body-parser');
const compression = require('compression');
const chalk = require('chalk');
const path = require('path');
const morgan = require('morgan');

const { API_ROOT, PORT } = require('./constants/config');

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

app.get('*', (req, res) => {
  res.sendFile(path.resolve(__dirname, 'client', 'build', 'index.html'));
});

app.listen(PORT, () => {
  console.log(chalk.green(`Ohara manager is running at port: ${PORT}`));
  console.log(chalk.blue(`Configurator API: ${API_ROOT}`));
});
