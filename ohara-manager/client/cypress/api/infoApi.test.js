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
// eslint is complaining about `expect(thing).to.be.undefined`

import * as infoApi from '../../src/api/infoApi';

describe('Info API', () => {
  it('fetchConfiguratorInfo', async () => {
    const infoRes = await infoApi.getConfiguratorInfo();
    const { mode, versionInfo } = infoRes;
    const { branch, version, user, revision, date } = versionInfo;

    // we're using fake configurator in our tests; so this value should be "FAKE"
    expect(mode).to.be.a('string');
    expect(mode).to.eq('FAKE');

    expect(branch).to.be.a('string');

    expect(version).to.be.a('string');

    expect(revision).to.be.a('string');

    expect(user).to.be.a('string');

    expect(date).to.be.a('string');
  });

  it('fetchServiceInfo', async () => {
    function expectResult(serviceName, data) {
      const { imageName, settingDefinitions } = data;
      expect(imageName).to.be.a('string');
      expect(imageName).to.include(serviceName);

      expect(settingDefinitions).to.be.an('array');
      expect(settingDefinitions.length > 0).to.be.true;
    }

    const infoZookeeper = await infoApi.getZookeeperInfo();
    expectResult(infoApi.service.zookeeper, infoZookeeper);

    const infoBroker = await infoApi.getBrokerInfo();
    expectResult(infoApi.service.broker, infoBroker);

    const infoWorker = await infoApi.getWorkerInfo();
    expectResult(infoApi.service.worker, infoWorker);
  });
});
