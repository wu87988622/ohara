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

import * as generate from '../../src/utils/generate';
import * as validateApi from '../../src/api/validateApi';
import { createServices, deleteAllServices } from '../utils';

const generateValidation = async () => {
  const { worker } = await createServices({
    withWorker: true,
    withBroker: true,
    withZookeeper: true,
    withNode: true,
  });
  const validation = {
    uri: generate.url(),
    url: generate.url(),
    user: generate.userName(),
    password: generate.password(),
    hostname: generate.domainName(),
    port: generate.port(),
    workerClusterKey: {
      name: worker.name,
      group: worker.group,
    },
  };
  return validation;
};

describe('Validate API', () => {
  beforeEach(() => deleteAllServices());

  it('validateHdfs', async () => {
    const hdfs = await generateValidation();
    const result = await validateApi.validateHdfs(hdfs);

    result.forEach(report => {
      expect(report.hostname).to.be.a('string');
      expect(report.message).to.be.a('string');
      expect(report.lastModified).to.be.a('number');
      expect(report.pass).to.be.true;
    });
  });

  it('validateRdb', async () => {
    const rdb = await generateValidation();
    const result = await validateApi.validateRdb(rdb);

    result.forEach(report => {
      expect(report.hostname).to.be.a('string');
      expect(report.message).to.be.a('string');
      expect(report.rdbInfo).to.be.an('object');
      expect(report.pass).to.be.true;
    });
  });

  it('validateFtp', async () => {
    const ftp = await generateValidation();
    const result = await validateApi.validateFtp(ftp);

    result.forEach(report => {
      expect(report.hostname).to.be.a('string');
      expect(report.message).to.be.a('string');
      expect(report.lastModified).to.be.a('number');
      expect(report.pass).to.be.true;
    });
  });

  it('validateNode', async () => {
    const node = await generateValidation();
    const result = await validateApi.validateNode(node);

    result.forEach(report => {
      expect(report.hostname).to.be.a('string');
      expect(report.message).to.be.a('string');
      expect(report.lastModified).to.be.a('number');
      expect(report.pass).to.be.true;
    });
  });

  it.skip('validateConnector', async () => {
    //TODO : implement stream part in " Add connector, pipeline API tests"
    // See https://github.com/oharastream/ohara/issues/3028
  });
});
