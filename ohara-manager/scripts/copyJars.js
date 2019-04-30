const fs = require('fs');

function copyJars() {
  fs.access('./client/cypress/fixtures/plugin/', function(err) {
    if (err) {
      fs.mkdirSync('./client/cypress/fixtures/plugin');
    }
    fs.copyFile(
      '../ohara-it/build/libs/ohara-it-source.jar',
      'client/cypress/fixtures/plugin/ohara-it-source.jar',
      err => {
        if (err) throw err;
      },
    );
    fs.copyFile(
      '../ohara-it/build/libs/ohara-it-sink.jar',
      'client/cypress/fixtures/plugin/ohara-it-sink.jar',
      err => {
        if (err) throw err;
      },
    );
  });

  fs.access('./client/cypress/fixtures/streamApp', function(err) {
    if (err) {
      fs.mkdirSync('./client/cypress/fixtures/streamApp');
    }
    fs.copyFile(
      '../ohara-it/build/libs/ohara-streamapp.jar',
      'client/cypress/fixtures/streamApp/ohara-streamapp.jar',
      err => {
        if (err) throw err;
      },
    );
  });
}

module.exports = copyJars;
