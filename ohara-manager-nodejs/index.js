const express = require('express');
const bodyParser = require('body-parser');
const compression = require('compression');
const chalk = require('chalk');
const path = require('path');

const PORT = process.env.PORT || 5050;
const app = express();

app.use(bodyParser.json());

// gzip static assets
app.use(compression());

app.use(express.static(path.resolve(__dirname, 'client', 'build')));

// APIs
require('./routes/authRoutes')(app);

app.get('*', (req, res) => {
  res.sendFile(path.resolve(__dirname, 'client', 'build', 'index.html'));
});

app.listen(PORT, () => {
  console.log(chalk.green(`Ohara manager is running at port: ${PORT}`));
});
