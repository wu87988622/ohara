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

import _ from 'lodash';

import { KIND, CELL_PROP, CELL_STATUS } from 'const';
import { createLink } from './cell';
import { getPipelineOnlyTopicDisplayNames } from '../PipelineUtils';
import * as generate from 'utils/generate';

export const getCellData = (cellOrView) => {
  const cell = _.has(cellOrView, 'model') ? cellOrView.model : cellOrView;

  if (cell.isElement()) {
    // We only expose necessary cell data to our users
    return {
      cellType: cell.get(CELL_PROP.cellType), // JointJS element type
      id: cell.get(CELL_PROP.id),
      name: cell.get(CELL_PROP.name),
      kind: cell.get(CELL_PROP.kind),
      displayName: cell.get(CELL_PROP.displayName),
      isTemporary: cell.get(CELL_PROP.isTemporary) || false,
      className: cell.get(CELL_PROP.className),
      position: cell.get(CELL_PROP.position),
      jarKey: cell.get(CELL_PROP.jarKey) || null,
      isShared: cell.get(CELL_PROP.isShared) || false,
      isSelected: cell.get(CELL_PROP.isSelected),
      status: cell.get(CELL_PROP.status),
      isIllegal: cell.get(CELL_PROP.isIllegal),
    };
  }

  const link = cell;
  return {
    cellType: link.get(CELL_PROP.cellType), // JointJS element type
    id: link.get(CELL_PROP.id),
    sourceId: link.get(CELL_PROP.source).id || null,
    targetId: link.get(CELL_PROP.target).id || null,
  };
};

export const showMenu = (elementView) => {
  elementView.showElement('menu').hover();

  if (elementView.model.get('showMetrics')) {
    return elementView.hideElement('metrics');
  }
  elementView.hideElement('status');
};

export const hideMenu = (elementView) => {
  elementView.unHover().hideElement('menu');

  if (elementView.model.get('showMetrics')) {
    return elementView.showElement('metrics');
  }
  elementView.showElement('status');
};

export const updateStatus = (cell, paperApi) => {
  if (cell.isLink()) {
    const link = cell;
    const sourceId = link.source().id;
    const targetId = link.target().id;

    const source = paperApi.getCell(sourceId);
    const target = paperApi.getCell(targetId);

    if (source) {
      paperApi.updateElement(sourceId, source);
    }

    if (target) {
      paperApi.updateElement(targetId, target);
    }
  }
};

export const createConnection = (params) => {
  const { sourceLink, eventLog, targetElementView, paperApi, graph } = params;

  const sourceId = sourceLink.get(CELL_PROP.source).id;
  const sourceType = graph.getCell(sourceId).attributes.kind;
  const sourceElement = graph.getCell(sourceId);

  const targetElement = targetElementView.model;
  const targetId = targetElement.get(CELL_PROP.id);
  const targetType = targetElement.get(CELL_PROP.kind);
  const targetDisplayName = targetElement.get(CELL_PROP.displayName);
  const targetStatus = targetElement.get(CELL_PROP.status);

  // Cell connection logic
  if (targetId === sourceId) {
    // A cell cannot connect to itself, not throwing a
    // message out here since the behavior is not obvious
  } else if (targetType === KIND.source) {
    eventLog.warning(`Target ${targetDisplayName} is a source!`);
  } else if (
    sourceType === targetType &&
    sourceType !== KIND.stream &&
    targetType !== KIND.stream
  ) {
    eventLog.warning(
      `Cannot connect a ${sourceType} to another ${targetType}, they both have the same type`,
    );
  } else if (targetStatus === CELL_STATUS.pending) {
    eventLog.warning(
      `The target ${targetDisplayName} is in pending state. Try to create the connection when the current action is completed`,
    );
  } else {
    const predecessors = graph.getPredecessors(targetElement);
    const targetHasSource = predecessors.some(
      (predecessor) => predecessor.attributes.kind === KIND.topic,
    );

    // Following are complex connection logic, each source and target
    // have different rules of whether or not it can be connected with
    // another cell
    if (sourceType === KIND.source && targetType === KIND.sink) {
      if (targetHasSource) {
        return eventLog.warning(
          `The target ${targetDisplayName} is already connected to a source`,
        );
      }
    }

    if (sourceType === KIND.source && targetType === KIND.stream) {
      if (targetHasSource) {
        return eventLog.warning(
          `The target ${targetDisplayName} is already connected to a source`,
        );
      }
    }

    if (sourceType === KIND.topic && targetType === KIND.sink) {
      if (targetHasSource) {
        return eventLog.warning(
          `The target ${targetDisplayName} is already connected to a source`,
        );
      }
    }

    if (sourceType === KIND.topic && targetType === KIND.stream) {
      if (targetHasSource) {
        return eventLog.warning(
          `The target ${targetDisplayName} is already connected to a source`,
        );
      }
    }

    if (sourceType === KIND.stream && targetType === KIND.sink) {
      if (targetHasSource) {
        return eventLog.warning(
          `The target ${targetDisplayName} is already connected to a source`,
        );
      }
    }

    if (sourceType === KIND.stream && targetType === KIND.stream) {
      if (targetHasSource) {
        return eventLog.warning(
          `The target ${targetDisplayName} is already connected to a source`,
        );
      }
    }

    // Create a topic between two cells that are about to connect
    // And only the below sources/targets have this behavior
    if (
      (sourceType === KIND.source && targetType === KIND.sink) ||
      (sourceType === KIND.source && targetType === KIND.stream) ||
      (sourceType === KIND.stream && targetType === KIND.sink) ||
      (sourceType === KIND.stream && targetType === KIND.stream)
    ) {
      const sourcePosition = sourceElement.position();
      const targetPosition = targetElement.position();

      // The topic will be placed at the center of two cells
      const topicX = (sourcePosition.x + targetPosition.x + 23) / 2;
      const topicY = (sourcePosition.y + targetPosition.y + 23) / 2;

      const PipelineOnlyTopicName = generate.serviceName();
      const displayName = getPipelineOnlyTopicDisplayNames(
        paperApi.getCells('topic'),
      );

      const topic = paperApi.addElement(
        {
          name: PipelineOnlyTopicName,
          displayName,
          kind: KIND.topic,
          className: KIND.topic,
          position: {
            x: topicX,
            y: topicY,
          },
        },
        {
          skipGraphEvents: true,
        },
      );

      const { id: topicId } = topic;

      sourceLink.target({ id: topicId });

      // Restore pointer event so the link can be clicked by mouse again
      sourceLink.attr('root/style', 'pointer-events: auto');

      const targetLink = createLink({
        sourceId: topicId,
        targetId: targetElement.id,
      });

      graph.addCell(targetLink);

      const result = {
        sourceElement: getCellData(sourceElement),
        firstLink: getCellData(sourceLink),
        topicElement: topic,
        secondeLink: getCellData(targetLink),
        targetElement: getCellData(targetElement),
      };

      return result;
    }

    // Link to the target cell
    sourceLink.target({ id: targetId });
    // Restore pointer event so the link can be clicked by mouse again
    sourceLink.attr('root/style', 'pointer-events: auto');

    return {
      sourceElement: getCellData(sourceElement),
      link: getCellData(sourceLink),
      targetElement: getCellData(targetElement),
    };
  }
};
