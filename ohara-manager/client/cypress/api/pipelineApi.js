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

describe.skip('Pipeline API', () => {
  // TODO: the test are skipped for now, need to enable and
  // refactor the tests later in #1749
  const pipelineName = 'name';
  const topicName = 'name';
  const wkName = 'name';

  it('createPipeline', () => {
    const params = {
      name: 'fakePipeline',
      rules: {},
      workerClusterName: 'wkName',
    };

    cy.testCreatePipeline(params).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { name, workerClusterName, objects } = result;

      expect(isSuccess).to.eq(true);
      expect(name).to.be.a('string');
      expect(workerClusterName).to.be.a('string');
      expect(objects).to.be.a('array');
    });
  });

  it('fetchPipeline', () => {
    cy.fetchPipeline(pipelineName).then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { name, workerClusterName, objects } = result;

      expect(isSuccess).to.eq(true);
      expect(name).to.be.a('string');
      expect(workerClusterName).to.be.a('string');
      expect(objects).to.be.a('array');
    });
  });

  it('fetchPipelines', () => {
    cy.fetchPipelines().then(res => {
      const {
        data: { isSuccess, result },
      } = res;
      const { name, workerClusterName, objects } = result[0];

      expect(isSuccess).to.eq(true);
      expect(name).to.be.a('string');
      expect(workerClusterName).to.be.a('string');
      expect(objects).to.be.a('array');
    });
  });

  it('updatePipeline', () => {
    const data = {
      params: {
        name: pipelineName,
        rules: {
          [topicName]: [],
        },
        workerClusterName: wkName,
      },
    };

    cy.updatePipeline(data).then(res => {
      const {
        data: { isSuccess, result },
      } = res;

      expect(isSuccess).to.eq(true);
      expect(result.name).to.be.a('string');
      expect(result.workerClusterName).to.be.a('string');
      expect(result.objects).to.be.a('array');
      expect(result.rules).to.be.a('object');
      expect(result.objects[0].id).to.eq(topicName);
      expect(result.objects[0].kind).to.eq('topic');
      expect(result.objects[0].name).to.eq(topicName);
    });
  });

  it('deletePipeline', () => {
    cy.testDeletePipeline(pipelineName).then(res => {
      const { data } = res;
      expect(data.isSuccess).to.eq(true);
    });
  });
});
