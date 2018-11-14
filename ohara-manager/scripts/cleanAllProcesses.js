const { exec } = require('child_process');

/* eslint-disable no-console */

// Stop all forever processes before running e2e tests
exec('./node_modules/.bin/forever stopall', (err, stdout) => {
  if (err) return;

  console.log(stdout.trim());
});

// Kill both server and client processes
exec('pkill -f index.js start.js', err => {
  if (err) return;
});
