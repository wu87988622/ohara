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

describe('Validate API', () => {
  // TODO: the test are skipped for now, need to enable and
  // refactor the tests later in #1749

  it.skip('validateConnector', () => {
    const params = {
      author: 'root',
      columns: [
        { dataType: 'STRING', name: 'test', newName: 'test', order: 1 },
      ],
      name: 'source',
      'connector.class': 'com.island.ohara.connector.ftp.FtpSource',
      'connector.name': 'source',
      'ftp.completed.folder': 'test',
      'ftp.encode': 'UTF-8',
      'ftp.error.folder': 'test',
      'ftp.hostname': 'test',
      'ftp.input.folder': 'test',
      'ftp.port': 20,
      'ftp.user.name': 'test',
      'ftp.user.password': 'test',
      kind: 'source',
      revision: '1e7da9544e6aa7ad2f9f2792ed8daf5380783727',
      'tasks.max': 1,
      topics: ['topicName'],
      version: '0.7.0-SNAPSHOT',
      workerClusterName: 'fakeWorkerName',
    };
    cy.validateConnector(params).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
      expect(data.result.errorCount).to.eq(0);
      expect(data.result).to.include.keys('errorCount', 'settings');
      expect(data.result.settings).to.be.a('array');
    });
  });
});
