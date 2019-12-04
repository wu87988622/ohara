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
import * as topicApi from '../../src/api/topicApi';
import * as connectorApi from '../../src/api/connectorApi';
import * as pipelineApi from '../../src/api/pipelineApi';
import { createServices, deleteAllServices } from '../utils';

const generatePipeline = async () => {
  const { node, broker, worker } = await createServices({
    withWorker: true,
    withBroker: true,
    withZookeeper: true,
    withNode: true,
  });
  const topic = {
    name: generate.serviceName({ prefix: 'topic' }),
    group: generate.serviceName({ prefix: 'group' }),
    nodeNames: [node.hostname],
    brokerClusterKey: {
      name: broker.name,
      group: broker.group,
    },
  };
  await topicApi.create(topic);

  const connectorSource = {
    name: generate.serviceName({ prefix: 'connector' }),
    group: generate.serviceName({ prefix: 'group' }),
    connector__class: connectorApi.connectorSources.perf,
    topicKeys: [{ name: topic.name, group: topic.group }],
    workerClusterKey: {
      name: worker.name,
      group: worker.group,
    },
  };
  await connectorApi.create(connectorSource);

  const connectorSink = {
    name: generate.serviceName({ prefix: 'connector' }),
    group: generate.serviceName({ prefix: 'group' }),
    connector__class: connectorApi.connectorSinks.console,
    topicKeys: [{ name: topic.name, group: topic.group }],
    workerClusterKey: {
      name: worker.name,
      group: worker.group,
    },
  };
  await connectorApi.create(connectorSink);

  const pipelineName = generate.serviceName({ prefix: 'pipeline' });
  const pipeline = {
    name: pipelineName,
    group: generate.serviceName({ prefix: 'group' }),
    flows: [
      {
        from: { name: connectorSource.name, group: connectorSource.group },
        to: [{ name: topic.name, group: topic.group }],
      },
      {
        from: { name: topic.name, group: topic.group },
        to: [{ name: connectorSink.name, group: connectorSink.group }],
      },
    ],
    tags: {
      name: pipelineName,
    },
  };
  return pipeline;
};

describe('Pipeline API', () => {
  beforeEach(() => deleteAllServices());

  it('createPipeline', async () => {
    const pipeline = await generatePipeline();
    const result = await pipelineApi.create(pipeline);
    expect(result.errors).to.be.undefined;

    const { flows, name, group, objects, lastModified, tags } = result.data;

    expect(flows).to.be.an('array');
    expect(flows.sort()).to.be.deep.eq(pipeline.flows.sort());

    expect(lastModified).to.be.a('number');

    expect(name).to.be.a('string');
    expect(name).to.eq(pipeline.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(pipeline.group);

    expect(objects).to.be.an('array');
    expect(objects).have.lengthOf(3);

    expect(tags.name).to.eq(pipeline.name);
  });

  it('fetchPipeline', async () => {
    const pipeline = await generatePipeline();
    await pipelineApi.create(pipeline);

    const result = await pipelineApi.get(pipeline);
    expect(result.errors).to.be.undefined;

    const { flows, name, group, objects, lastModified, tags } = result.data;

    expect(flows).to.be.an('array');
    expect(flows.sort()).to.be.deep.eq(pipeline.flows.sort());

    expect(lastModified).to.be.a('number');

    expect(name).to.be.a('string');
    expect(name).to.eq(pipeline.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(pipeline.group);

    expect(objects).to.be.an('array');
    expect(objects).have.lengthOf(3);

    expect(tags.name).to.eq(pipeline.name);
  });

  it('fetchPipelines', async () => {
    const pipelineOne = await generatePipeline();
    const pipelineTwo = await generatePipeline();

    await pipelineApi.create(pipelineOne);
    await pipelineApi.create(pipelineTwo);

    const result = await pipelineApi.getAll();
    expect(result.errors).to.be.undefined;

    const pipelines = result.data.map(pipeline => pipeline.name);
    expect(pipelines.includes(pipelineOne.name)).to.be.true;
    expect(pipelines.includes(pipelineTwo.name)).to.be.true;
  });

  it('deletePipeline', async () => {
    const pipeline = await generatePipeline();
    await pipelineApi.create(pipeline);

    await pipelineApi.remove(pipeline);
    const result = await pipelineApi.getAll();
    expect(result.errors).to.be.undefined;

    expect(result.data.includes(pipeline.name)).to.be.false;
  });

  it('updatePipeline', async () => {
    const pipeline = await generatePipeline();
    const newParams = {
      flows: [],
    };
    const newPipeline = { ...pipeline, ...newParams };

    await pipelineApi.create(pipeline);

    const result = await pipelineApi.update(newPipeline);
    expect(result.errors).to.be.undefined;

    const { flows, name, group, objects, lastModified, tags } = result.data;

    expect(flows).to.be.an('array');
    expect(flows).have.lengthOf(0);

    expect(lastModified).to.be.a('number');

    expect(name).to.be.a('string');
    expect(name).to.eq(pipeline.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(pipeline.group);

    expect(objects).to.be.an('array');
    expect(objects).have.lengthOf(0);

    expect(tags.name).to.eq(pipeline.name);
  });

  it('refreshPipeline', async () => {
    const pipeline = await generatePipeline();
    const result = await pipelineApi.create(pipeline);
    expect(result.errors).to.be.undefined;

    const source = result.data.objects.find(obj => obj.kind === 'source');

    await connectorApi.stop({ name: source.name, group: source.group });
    await connectorApi.remove({ name: source.name, group: source.group });

    const refreshResult = await pipelineApi.refresh(pipeline);
    expect(refreshResult.errors).to.be.undefined;

    const {
      flows,
      name,
      group,
      objects,
      lastModified,
      tags,
    } = refreshResult.data;

    expect(flows).to.be.an('array');
    expect(flows).have.lengthOf(1);

    expect(lastModified).to.be.a('number');

    expect(name).to.be.a('string');
    expect(name).to.eq(pipeline.name);

    expect(group).to.be.a('string');
    expect(group).to.eq(pipeline.group);

    expect(objects).to.be.an('array');
    expect(objects).have.lengthOf(2);

    expect(tags.name).to.eq(pipeline.name);
  });
});
