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

import * as joint from 'jointjs';

import { KIND } from 'const';
import { TopicGraph } from '../Graph/Topic';

export const updateCurrentCell = currentCell => {
  if (currentCell.current) {
    currentCell.current = {
      cellView: currentCell.current.cellView,
      bBox: {
        ...currentCell.current.cellView.getBBox(),
        ...currentCell.current.cellView.getBBox().center(),
      },
    };
  }
};

export const createConnection = params => {
  const {
    currentLink: sourceLink,
    showMessage,
    resetLink,
    graph,
    paper,
    cellView,
    setInitToolboxList,
  } = params;

  const targetCell = cellView.model;
  const targetId = targetCell.get('id');
  const targetType = targetCell.get('classType');
  const targetTitle = targetCell.get('title');
  const targetConnectedLinks = graph.current.getConnectedLinks(targetCell);

  const sourceId = sourceLink.get('source').id;
  const sourceType = graph.current.getCell(sourceId).attributes.classType;
  const sourceCell = graph.current.getCell(sourceId);
  const sourceTitle = sourceCell.get('title');

  const isLoopLink = () => {
    return targetConnectedLinks.some(link => {
      return (
        sourceId === link.get('source').id || sourceId === link.get('target').id
      );
    });
  };

  const handleError = message => {
    showMessage(message);

    // Reset a link when connection failed
    resetLink();
  };

  // Cell connection logic
  if (targetId === sourceId) {
    // A cell cannot connect to itself, not throwing a
    // message out here since the behavior is not obvious
    resetLink();
  } else if (targetType === KIND.source) {
    handleError(`Target ${targetTitle} is a source!`);
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
    const predecessors = graph.current.getPredecessors(targetCell);
    const successors = graph.current.getSuccessors(sourceCell);
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
          `The source ${sourceTitle} is already connected to a target`,
        );
      }
      if (targetHasSource) {
        return handleError(
          `The target ${targetTitle} is already connected to a source`,
        );
      }
    }

    if (sourceType === KIND.source && targetType === KIND.stream) {
      if (sourceHasTarget) {
        return handleError(
          `The source ${sourceTitle} is already connected to a target`,
        );
      }
      if (targetHasSource) {
        return handleError(
          `The target ${targetTitle} is already connected to a source`,
        );
      }
    }

    if (sourceType === KIND.source && targetType === KIND.topic) {
      if (sourceHasTarget) {
        return handleError(
          `The source ${sourceTitle} is already connected to a target`,
        );
      }
    }

    if (sourceType === KIND.topic && targetType === KIND.sink) {
      if (targetHasSource) {
        return handleError(
          `The target ${targetTitle} is already connected to a source`,
        );
      }
    }

    if (sourceType === KIND.topic && targetType === KIND.stream) {
      if (targetHasSource) {
        return handleError(
          `The target ${targetTitle} is already connected to a source`,
        );
      }
    }

    if (sourceType === KIND.stream && targetType === KIND.topic) {
      if (sourceHasTarget) {
        return handleError(
          `The source ${sourceTitle} is already connected to a target`,
        );
      }
    }

    if (sourceType === KIND.stream && targetType === KIND.sink) {
      if (sourceHasTarget) {
        return handleError(
          `The source ${sourceTitle} is already connected to a sink`,
        );
      }

      if (targetHasSource) {
        return handleError(
          `The target ${targetTitle} is already connected to a source`,
        );
      }
    }

    if (sourceType === KIND.stream && targetType === KIND.stream) {
      if (sourceHasTarget) {
        return handleError(
          `The source ${sourceTitle} is already connected to a sink`,
        );
      }

      if (targetHasSource) {
        return handleError(
          `The target ${targetTitle} is already connected to a source`,
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
      const sourcePosition = sourceCell.position();
      const targetPosition = targetCell.position();

      // The topic will be placed at the center of two cells
      const topicX = (sourcePosition.x + targetPosition.x + 23) / 2;
      const topicY = (sourcePosition.y + targetPosition.y + 23) / 2;

      graph.current.addCell(
        TopicGraph({
          cellInfo: {
            position: {
              x: topicX,
              y: topicY,
            },
            classType: KIND.topic,
            className: 'privateTopic', // This topic should always be a private topic
          },
          graph,
          paper,
        }),
      );

      // Since we just added this topic into graph, it's the last cell
      const topicId = graph.current.getLastCell().attributes.id;
      sourceLink.target({ id: topicId });

      // Restore pinter event so the link can be clicked by mouse again
      sourceLink.attr('root/style', 'pointer-events: auto');

      const newLink = new joint.shapes.standard.Link();
      newLink.source({ id: topicId });
      newLink.target({ id: targetCell.id });
      newLink.attr({ line: { stroke: '#9e9e9e' } });
      newLink.addTo(graph.current);

      // There's a bug causes by not re-initializing JointJS' HTML element
      // Because we're using these custom HTML elements both in `TopicGraph`
      // Component as well as Toolbox. And so we're manually re-initializing
      // Toolbox to prevent the bug
      setInitToolboxList(prevState => prevState + 1);
      return;
    }

    // Link to the target cell
    sourceLink.target({ id: targetCell.id });
    // Restore pinter event so the link can be clicked by mouse again
    sourceLink.attr('root/style', 'pointer-events: auto');
  }
};
