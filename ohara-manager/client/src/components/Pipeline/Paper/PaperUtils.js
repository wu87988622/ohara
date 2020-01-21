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

import { KIND } from 'const';
import { createLink } from './cell';
import { getPrivateTopicDisplayNames } from '../PipelineUtils';
import * as generate from 'utils/generate';

function isView(cell) {
  if (cell.$box === undefined) return false;

  return true;
}

export const getReturnedData = cell => {
  const isCellView = isView(cell);

  const _cell = isCellView ? cell.model : cell;

  if (_cell.isElement()) {
    return {
      cellType: _cell.get('type'), // JointJS element type
      id: _cell.get('id'),
      name: _cell.get('name'),
      kind: _cell.get('classType'),
      displayName: _cell.get('displayName'),
      isTemporary: _cell.get('isTemporary') || false,
      className: _cell.get('className'),
      jarKey: _cell.get('jarKey') || null,
    };
  }

  const link = _cell;
  return {
    id: link.get('id'),
    sourceId: link.get('source').id || null,
    targetId: link.get('target').id || null,
  };
};

export const createConnection = params => {
  const {
    sourceLink,
    showMessage,
    targetElementView,
    paperApi,
    graph,
  } = params;

  const sourceId = sourceLink.get('source').id;
  const sourceType = graph.getCell(sourceId).attributes.classType;
  const sourceElement = graph.getCell(sourceId);
  const sourceDisplayName = sourceElement.get('displayName');

  const targetElement = targetElementView.model;
  const targetId = targetElement.get('id');
  const targetType = targetElement.get('classType');
  const targetDisplayName = targetElement.get('displayName');
  const targetConnectedLinks = graph.getConnectedLinks(targetElement);

  const isLoopLink = () => {
    return targetConnectedLinks.some(link => {
      return (
        sourceId === link.get('source').id || sourceId === link.get('target').id
      );
    });
  };

  const handleError = message => {
    showMessage(message);
  };

  // Cell connection logic
  if (targetId === sourceId) {
    // A cell cannot connect to itself, not throwing a
    // message out here since the behavior is not obvious
  } else if (targetType === KIND.source) {
    handleError(`Target ${targetDisplayName} is a source!`);
  } else if (
    sourceType === targetType &&
    (sourceType !== KIND.stream && targetType !== KIND.stream)
  ) {
    handleError(
      `Cannot connect a ${sourceType} to another ${targetType}, they both have the same type`,
    );
  } else if (isLoopLink()) {
    handleError(`A connection is already in place for these two cells`);
  } else {
    const predecessors = graph.getPredecessors(targetElement);
    const successors = graph.getSuccessors(sourceElement);
    const sourceHasTarget = successors.some(
      successor => successor.attributes.classType === KIND.topic,
    );
    const targetHasSource = predecessors.some(
      predecessor => predecessor.attributes.classType === KIND.topic,
    );

    // Following are complex connection logic, each source and target
    // have different rules of whether or not it can be connected with
    // another cell
    if (sourceType === KIND.source && targetType === KIND.sink) {
      if (sourceHasTarget) {
        return handleError(
          `The source ${sourceDisplayName} is already connected to a target`,
        );
      }
      if (targetHasSource) {
        return handleError(
          `The target ${targetDisplayName} is already connected to a source`,
        );
      }
    }

    if (sourceType === KIND.source && targetType === KIND.stream) {
      if (sourceHasTarget) {
        return handleError(
          `The source ${sourceDisplayName} is already connected to a target`,
        );
      }
      if (targetHasSource) {
        return handleError(
          `The target ${targetDisplayName} is already connected to a source`,
        );
      }
    }

    if (sourceType === KIND.source && targetType === KIND.topic) {
      if (sourceHasTarget) {
        return handleError(
          `The source ${sourceDisplayName} is already connected to a target`,
        );
      }
    }

    if (sourceType === KIND.topic && targetType === KIND.sink) {
      if (targetHasSource) {
        return handleError(
          `The target ${targetDisplayName} is already connected to a source`,
        );
      }
    }

    if (sourceType === KIND.topic && targetType === KIND.stream) {
      if (targetHasSource) {
        return handleError(
          `The target ${targetDisplayName} is already connected to a source`,
        );
      }
    }

    if (sourceType === KIND.stream && targetType === KIND.topic) {
      if (sourceHasTarget) {
        return handleError(
          `The source ${sourceDisplayName} is already connected to a target`,
        );
      }
    }

    if (sourceType === KIND.stream && targetType === KIND.sink) {
      if (sourceHasTarget) {
        return handleError(
          `The source ${sourceDisplayName} is already connected to a sink`,
        );
      }

      if (targetHasSource) {
        return handleError(
          `The target ${targetDisplayName} is already connected to a source`,
        );
      }
    }

    if (sourceType === KIND.stream && targetType === KIND.stream) {
      if (sourceHasTarget) {
        return handleError(
          `The source ${sourceDisplayName} is already connected to a sink`,
        );
      }

      if (targetHasSource) {
        return handleError(
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

      const privateTopicName = generate.serviceName();
      const displayName = getPrivateTopicDisplayNames(
        paperApi.getCells('topic'),
      );

      const topic = paperApi.addElement({
        name: privateTopicName,
        graph,
        displayName,
        classType: KIND.topic,
        className: 'privateTopic',
        position: {
          x: topicX,
          y: topicY,
        },
      });

      const { id: topicId } = topic;

      sourceLink.target({ id: topicId });

      // Restore pinter event so the link can be clicked by mouse again
      sourceLink.attr('root/style', 'pointer-events: auto');

      const targetLink = createLink({
        sourceId: topicId,
        targetId: targetElement.id,
      });

      graph.addCell(targetLink);

      const result = {
        sourceElement: getReturnedData(sourceElement),
        firstLink: getReturnedData(sourceLink),
        topicElement: topic,
        secondeLink: getReturnedData(targetLink),
        targetElement: getReturnedData(targetElement),
      };

      return result;
    }

    // Link to the target cell
    sourceLink.target({ id: targetId });
    // Restore pinter event so the link can be clicked by mouse again
    sourceLink.attr('root/style', 'pointer-events: auto');

    return {
      sourceElement: getReturnedData(sourceElement),
      link: getReturnedData(sourceLink),
      targetElement: getReturnedData(targetElement),
    };
  }
};
