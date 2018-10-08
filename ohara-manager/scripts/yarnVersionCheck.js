const { exec } = require('child_process');

/* eslint-disable no-console */
exec('yarn -v', (err, stdout) => {
  if (err) throw err;

  const yarnVersion = stdout.trim();

  if (yarnVersion !== '1.7.0') {
    throw new Error(
      `Ohara Manger requires yarn 1.7.0, but you're using ${yarnVersion}`,
    );
  }

  console.log(`👌 Yarn version check passed! You're using ${yarnVersion}`);
  console.log('📦 Installing Ohara Manager dependencies');
});
