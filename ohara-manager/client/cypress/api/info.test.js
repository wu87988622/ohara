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

/* eslint-disable no-unused-expressions */

import * as infoApi from '../../src/api/infoApi';

describe.skip('Info API', () => {
  it('fetchConfiguratorInfo', () => {
    infoApi.fetchConfiguratorInfo().then(response => {
      const {
        data: { isSuccess, result },
      } = response;

      const { mode, versionInfo } = result;
      const { branch, version, user, revision, date } = versionInfo;

      expect(isSuccess).to.eq(true);

      // we're using fake configurator in our tests; so this value should be "FAKE"
      expect(mode).to.be.a('string');
      expect(mode).to.eq('FAKE');

      expect(branch).to.be.a('string');

      expect(version).to.be.a('string');

      expect(revision).to.be.a('string');

      expect(user).to.be.a('string');

      expect(date).to.be.a('string');
    });
  });

  it('fetchServiceInfo', () => {
    function expectResult(serviceName, response) {
      const {
        data: { isSuccess, result },
      } = response;

      const { imageName, definitions } = result;

      expect(isSuccess).to.eq(true);

      expect(imageName).to.be.a('string');
      expect(imageName).to.include(serviceName);

      expect(definitions).to.be.an('array');
      expect(definitions.length > 0).to.be.true;

      // expect each service type should have it's own objectKey: {name, group}
      const objectKeyDefinitions = definitions
        .map(definition => definition.key)
        .filter(key => key === 'group' || key === 'name');
      expect(objectKeyDefinitions.length).to.eq(2);
    }

    const serviceNames = ['zookeeper', 'broker', 'worker'];
    serviceNames.forEach(serviceName => {
      infoApi
        .fetchServiceInfo(serviceName)
        .then(response => expectResult(serviceName, response));
    });
  });
});
