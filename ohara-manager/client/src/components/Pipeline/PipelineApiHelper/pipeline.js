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
import { KIND, CELL_STATUS } from 'const';

const pipeline = () => {
  const { updatePipeline } = context.usePipelineActions();
  const { deleteConnector, stopConnector } = context.useConnectorActions();
  const { deleteTopic } = context.useTopicActions();
  const { deleteStream, stopStream } = context.useStreamActions();
  const { currentPipeline } = context.useWorkspace();
  const { data: currentConnectors } = context.useConnectorState();
  const { data: currentTopic } = context.useTopicState();
  const { data: currentStream } = context.useStreamState();

  const updateCells = paperApi => {
    const cellsJson = {
      cells: paperApi.toJson().cells.filter(cell => !cell.isTemporary),
    };

    const endpoints = cellsJson.cells
      .filter(cell => cell.type === 'html.Element')
      .map(cell => {
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
    const apiTopics = currentTopic.map(api => api.name);
    const apiStreams = currentStream.map(api => api.name);

    const legacyConnectors = checkCell(apiConnectors, paperConnectors);
    const legacyTopics = checkCell(apiTopics, paperTopics);
    const legacyStream = checkCell(apiStreams, paperStreams);

    for (const connector of legacyConnectors.legacyApiData) {
      await stopConnector(connector);
      await deleteConnector(connector);
    }
    for (const topic of legacyTopics.legacyApiData) {
      const findSharedTopic = currentTopic
        .filter(api => api.tags.isShared)
        .find(topic => topic.name === topic);
      if (findSharedTopic) continue;
      await deleteTopic(topic);
    }
    for (const stream of legacyStream.legacyApiData) {
      await stopStream(stream);
      await deleteStream(stream);
    }

    const updateConnectorEndpoints = currentPipeline.endpoints.filter(
      endpoint =>
        !legacyConnectors.legacyPaperData.includes(endpoint.name) &&
        (endpoint.kind === KIND.source || endpoint.kind === KIND.sink),
    );
    const updateTopicEndpoints = currentPipeline.endpoints.filter(
      endpoint =>
        !legacyTopics.legacyPaperData.includes(endpoint.name) &&
        endpoint.kind === KIND.topic,
    );
    const updateStreamEndpoints = currentPipeline.endpoints.filter(
      endpoint =>
        !legacyStream.legacyPaperData.includes(endpoint.name) &&
        endpoint.kind === KIND.stream,
    );
    const updateEndpoints = [
      ...updateConnectorEndpoints,
      ...updateTopicEndpoints,
      ...updateStreamEndpoints,
    ];

    const updateConnectorsTags = _.get(currentPipeline, 'tags.cells', [])
      .filter(cell => cell.kind === KIND.source || cell.kind === KIND.sink)
      .filter(cell => !legacyConnectors.legacyPaperData.includes(cell.name))
      .map(cell => updateStatus(cell, currentConnectors));
    const updateTopicsTags = _.get(currentPipeline, 'tags.cells', [])
      .filter(cell => cell.kind === KIND.topic)
      .filter(cell => !legacyTopics.legacyPaperData.includes(cell.name))
      .map(cell => updateStatus(cell, currentTopic));
    const updateStreamsTags = _.get(currentPipeline, 'tags.cells', [])
      .filter(cell => cell.kind === KIND.stream)
      .filter(cell => !legacyStream.legacyPaperData.includes(cell.name))
      .map(cell => updateStatus(cell, currentStream));
    const links = _.get(currentPipeline, 'tags.cells', []).filter(
      cell => cell.type === 'standard.Link',
    );
    let updateTags = [
      ...updateConnectorsTags,
      ...updateTopicsTags,
      ...updateStreamsTags,
      ...links,
    ];

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

const updateStatus = (cell, currentCells) => {
  const currentCell = currentCells.find(
    currentCell => currentCell.name === cell.name,
  );

  if (!currentCell) {
    cell.status = CELL_STATUS.stopped;
  } else {
    cell.status = _.get(currentCell, 'state', CELL_STATUS.stopped);
  }
  return cell;
};

export default pipeline;
