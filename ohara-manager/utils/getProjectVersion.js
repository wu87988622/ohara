const { existsSync, readFileSync } = require('fs');
const { resolve } = require('path');

// TODO: OHARA-914 figure out a better way to obtain project version

// Get project version from gradle.properties if it's in the Ohara manager root
// otherwise, get version from package.json
const getProjectVersion = () => {
  const gradlePropPath = resolve('./gradle.properties');
  const pkgJsonPath = resolve('./package.json');

  if (existsSync(gradlePropPath)) {
    const fileContent = readFileSync(gradlePropPath, 'utf8');
    const lastLine = fileContent.split('\n')[1];
    const version = lastLine.split('=')[1];
    return version;
  }

  const json = require(pkgJsonPath);
  return json.version;
};

module.exports = getProjectVersion;
