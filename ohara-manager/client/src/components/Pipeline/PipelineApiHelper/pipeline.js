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

import * as context from 'context';
import * as _ from 'lodash';
import { KIND } from 'const';

const pipeline = () => {
  const { updatePipeline } = context.usePipelineActions();
  const { deleteConnector } = context.useConnectorActions();
  const { deleteTopic } = context.useTopicActions();
  const { deleteStream } = context.useStreamActions();
  const { currentPipeline } = context.useWorkspace();
  const { data: currentConnectors } = context.useConnectorState();
  const { data: currentTopic } = context.useTopicState();
  const { data: currentStream } = context.useStreamState();

  const updateCells = paperApi => {
    const cellsJson = {
      cells: paperApi.toJson().cells.filter(cell => !cell.isTemporary),
    };
    const endpoints = cellsJson.cells.map(cell => {
      return { name: cell.name, kind: cell.kind };
    });
    updatePipeline({
      name: currentPipeline.name,
      endpoints,
      tags: {
        ...cellsJson,
      },
    });
  };

  const checkCells = async paperApi => {
    const paperConnectors = currentPipeline.endpoints
      .filter(
        endpoint =>
          endpoint.kind === KIND.sink || endpoint.kind === KIND.source,
      )
      .map(endpoint => endpoint.name);
    const paperTopics = currentPipeline.endpoints
      .filter(endpoint => endpoint.kind === KIND.topic)
      .map(endpoint => endpoint.name);
    const paperStreams = currentPipeline.endpoints
      .filter(endpoint => endpoint.kind === KIND.stream)
      .map(endpoint => endpoint.name);

    const apiConnectors = currentConnectors.map(api => api.name);
    const apiTopics = currentTopic
      .filter(api => !api.tags.isShared)
      .map(api => api.name);
    const apiStreams = currentStream.map(api => api.name);

    const legacyConnectors = checkCell(apiConnectors, paperConnectors);
    const legacyTopics = checkCell(apiTopics, paperTopics);
    const legacyStream = checkCell(apiStreams, paperStreams);

    for (const connector of legacyConnectors.legacyApiData) {
      await deleteConnector(connector);
    }
    for (const topic of legacyTopics.legacyApiData) {
      await deleteTopic(topic);
    }
    for (const stream of legacyStream.legacyApiData) {
      await deleteStream(stream);
    }

    const updateEndpoints = currentPipeline.endpoints
      .filter(
        endpoint =>
          endpoint.name !== legacyConnectors.legacyPaperData &&
          (endpoint.kind === KIND.source || endpoint.kind === KIND.sink),
      )
      .filter(
        endpoint =>
          endpoint.name !== legacyTopics.legacyPaperData &&
          endpoint.kind === KIND.topic,
      )
      .filter(
        endpoint =>
          endpoint.name !== legacyStream.legacyPaperData &&
          endpoint.kind === KIND.stream,
      );

    const updateTags = _.get(currentPipeline, 'tags.cells', [])
      .filter(
        cell =>
          cell.name !== legacyConnectors.legacyPaperData &&
          (cell.kind === KIND.source || cell.kind === KIND.sink),
      )
      .filter(
        cell =>
          cell.name !== legacyTopics.legacyPaperData &&
          cell.kind === KIND.topic,
      )
      .filter(
        cell =>
          cell.name !== legacyStream.legacyPaperData &&
          cell.kind === KIND.stream,
      );
    if (updateTags.length > 0 || updateTags.length > 0) {
      updatePipeline({
        name: currentPipeline.name,
        endpoints: updateEndpoints,
        tags: {
          cells: updateTags,
        },
      });
      paperApi.loadGraph({ cells: updateTags });
    } else {
      paperApi.loadGraph({ cells: _.get(currentPipeline, 'tags.cells', []) });
    }
  };

  const checkCell = (apiData, paperData) => {
    const legacyApiData =
      paperData.length === 0
        ? apiData
        : apiData.filter(api => !paperData.includes(api));
    const legacyPaperData =
      apiData.length === 0
        ? paperData
        : paperData.filter(paper => !apiData.includes(paper));

    return { legacyApiData, legacyPaperData };
  };

  return { updateCells, checkCells };
};

export default pipeline;
