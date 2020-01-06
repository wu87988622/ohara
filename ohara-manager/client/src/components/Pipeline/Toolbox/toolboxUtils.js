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

import React from 'react';
import { renderToString } from 'react-dom/server';
import { isNull } from 'lodash';
import FlightTakeoffIcon from '@material-ui/icons/FlightTakeoff';
import FlightLandIcon from '@material-ui/icons/FlightLand';
import StorageIcon from '@material-ui/icons/Storage';
import WavesIcon from '@material-ui/icons/Waves';
import * as joint from 'jointjs';
import $ from 'jquery';

import { KIND } from 'const';
import * as generate from 'utils/generate';
import ConnectorGraph from '../Graph/Connector/ConnectorGraph';
import TopicGraph from '../Graph/Topic/TopicGraph';
import { AddPublicTopicIcon } from 'components/common/Icon';

export const createToolboxList = params => {
  const {
    connectors,
    sourceGraph,
    topicGraph,
    streamGraph,
    sinkGraph,
    searchResults,
  } = params;

  const { sources, topics, streams, sinks } = connectors;

  // Create a custom element.
  joint.shapes.html = {};
  joint.shapes.html.Element = joint.shapes.basic.Rect.extend({
    defaults: joint.util.deepSupplement(
      {
        type: 'html.Element',
        attrs: {
          rect: { stroke: 'none', fill: 'transparent' },
        },
      },
      joint.shapes.basic.Rect.prototype.defaults,
    ),
  });

  // Create a custom view for that element that displays an HTML div above it.
  joint.shapes.html.ElementView = joint.dia.ElementView.extend({
    template: `<div class="item">
        <span class="icon"></span>
        <span class="display-name"></span>
      </div>`,

    init() {
      this.listenTo(this.model, 'change', this.updateBox);
    },
    onRender() {
      const boxMarkup = joint.util.template(this.template)();
      const $box = (this.$box = $(boxMarkup));
      this.listenTo(this.paper, 'scale translate', this.updateBox);
      $box.appendTo(this.paper.el);

      this.updateBox();
      return this;
    },

    updateBox() {
      // Updating the HTML with a data stored in the cell model.
      this.$box.find('.display-name').text(this.model.get('name'));
      this.$box.find('.icon').html(this.model.get('icon'));
    },
  });

  const sourceIcon = renderToString(<FlightTakeoffIcon color="action" />);
  const sinkIcon = renderToString(<FlightLandIcon color="action" />);
  const streamIcon = renderToString(<WavesIcon color="action" />);
  const AddPrivateTopic = renderToString(<StorageIcon color="action" />);

  // Custom icon, so we need to pass some props...
  const AddPublicTopic = renderToString(
    <AddPublicTopicIcon className="public-topic" width={23} height={22} />,
  );

  const displaySources = isNull(searchResults)
    ? sources
    : searchResults.sources;

  // Create JointJS elements and add them to the graph as usual.
  displaySources.forEach((source, index) => {
    sourceGraph.current.addCell(
      new joint.shapes.html.Element({
        position: { x: 10, y: index * 40 },
        size: { width: 272 - 8 * 2, height: 40 },
        name: source.name,
        classType: source.classType,
        icon: sourceIcon,
        className: source.className,
      }),
    );
  });

  const displayTopics = isNull(searchResults) ? topics : searchResults.topics;

  displayTopics.forEach((topic, index) => {
    topicGraph.current.addCell(
      new joint.shapes.html.Element({
        position: { x: 10, y: index * 40 },
        size: { width: 272 - 8 * 2, height: 40 },
        name: topic.name,
        classType: topic.classType,
        className: index === 0 ? 'privateTopic' : 'publicTopic',
        icon: topic.type === 'private' ? AddPrivateTopic : AddPublicTopic,
      }),
    );
  });

  const displayStreams = isNull(searchResults)
    ? streams
    : searchResults.streams;

  displayStreams.forEach((stream, index) => {
    streamGraph.current.addCell(
      new joint.shapes.html.Element({
        position: { x: 10, y: index * 40 },
        size: { width: 272 - 8 * 2, height: 40 },
        name: stream.name,
        classType: stream.classType,
        icon: streamIcon,
        className: stream.className,
      }),
    );
  });

  const displaySinks = isNull(searchResults) ? sinks : searchResults.sinks;

  displaySinks.forEach((sink, index) => {
    sinkGraph.current.addCell(
      new joint.shapes.html.Element({
        position: { x: 10, y: index * 40 },
        size: { width: 272 - 8 * 2, height: 40 },
        name: sink.name,
        classType: sink.classType,
        icon: sinkIcon,
        className: sink.className,
      }),
    );
  });
};

export const enableDragAndDrop = params => {
  const {
    toolPapers,
    graph,
    paper,
    setCellInfo,
    setIsOpen: openAddConnectorDialog,
    currentPipeline,
    topicsData,
    createTopic,
    stopTopic,
    deleteTopic,
    updatePipeline,
  } = params;

  toolPapers.forEach(toolPaper => {
    // Add "hover" state in items, I cannot figure out how to do
    // this when initializing the HTML elements...
    toolPaper.on('cell:mouseenter', cellView => {
      cellView.$box.css('backgroundColor', 'rgba(0, 0, 0, 0.08)');
    });

    toolPaper.on('cell:mouseleave', cellView => {
      cellView.$box.css('backgroundColor', 'transparent');
    });

    // Create "flying papers", which enable drag and drop feature
    toolPaper.on('cell:pointerdown', (cellView, event, x, y) => {
      $('#paper').append('<div id="flying-paper" class="flying-paper"></div>');
      const flyingGraph = new joint.dia.Graph();
      new joint.dia.Paper({
        el: $('#flying-paper'),
        width: 160,
        height: 50,
        model: flyingGraph,
        cellViewNamespace: joint.shapes,
        interactive: false,
      });

      const flyingShape = cellView.model.clone();
      const position = cellView.model.position();
      const offset = {
        x: x - position.x,
        y: y - position.y,
      };

      flyingShape.position(0, 0);
      flyingGraph.addCell(flyingShape);

      $('#flying-paper').offset({
        left: event.pageX - offset.x,
        top: event.pageY - offset.y,
      });

      $('#paper').on('mousemove.fly', event => {
        $('#flying-paper').offset({
          left: event.pageX - offset.x,
          top: event.pageY - offset.y,
        });
      });

      $('#paper').on('mouseup.fly', async event => {
        const x = event.pageX;
        const y = event.pageY;
        const target = paper.current.$el.offset();

        const isInsidePaper =
          x > target.left &&
          x < target.left + paper.current.$el.width() &&
          y > target.top &&
          y < target.top + paper.current.$el.height();

        // Dropped over paper ?
        if (isInsidePaper) {
          const localPoint = paper.current.paperToLocalPoint(
            paper.current.translate(),
          );
          const scale = paper.current.scale();
          const newX = (x - target.left - offset.x) / scale.sx + localPoint.x;
          const newY = (y - target.top - offset.y) / scale.sy + localPoint.y;

          const {
            classType,
            className,
            name,
            icon,
          } = cellView.model.attributes;

          const isTopic = classType === KIND.topic;

          // These info will be used when creating a cell
          setCellInfo(prevState => ({
            ...prevState,
            classType,
            className,
            displayedClassName: name,
            icon,
            position: {
              ...prevState.position,
              x: newX,
              y: newY,
            },
          }));

          const params = {
            position: { x: newX, y: newY },
            isTemporary: !isTopic, // Display a temp cell for all connectors but topic
            title: !isTopic ? 'newgraph' : name, // Same here, display a temp name for all connectors except topic
            graph,
            paper,
            cellInfo: {
              classType,
              className,
              icon,
              displayedClassName: name,
              position: {
                x: newX,
                y: newY,
              },
            },
          };

          const getPrivateTopicDisplayNames = topics => {
            const topicIndex = topics
              .map(topic => topic.tags)
              .filter(topic => topic.type === 'private')
              .map(topic => topic.displayName.replace('T', ''))
              .sort();

            if (topicIndex.length === 0) return 'T1';
            return `T${Number(topicIndex.pop()) + 1}`;
          };

          if (isTopic) {
            const privateTopicName = generate.serviceName({ length: 5 });
            const isPublicTopic = className === 'publicTopic';
            const displayName = isPublicTopic
              ? name
              : getPrivateTopicDisplayNames(topicsData);

            let topicGroup;
            if (!isPublicTopic) {
              const { data } = await createTopic({
                name: privateTopicName,
                tags: {
                  type: 'private',
                  displayName,
                },
              });

              if (data) {
                topicGroup = data.group;
              }
            }

            await updatePipeline({
              name: currentPipeline.name,
              endpoints: [
                ...currentPipeline.endpoints,
                {
                  name: privateTopicName,
                  group: topicGroup,
                  kind: KIND.topic,
                },
              ],
            });

            graph.current.addCell(
              TopicGraph({
                name: privateTopicName,
                graph,
                paper,
                stopTopic,
                deleteTopic,
                title: displayName,
                cellInfo: params.cellInfo,
              }),
            );
          } else {
            openAddConnectorDialog(true);

            // A temporary cell which gives users a better idea of
            // where the graph will be added at. It will be removed
            // once the real graph is added
            const newCell = ConnectorGraph(params);
            graph.current.addCell(newCell);
          }
        }
        // Clean up
        $('#paper')
          .off('mousemove.fly')
          .off('mouseup.fly');
        flyingShape.remove();

        $('#flying-paper').remove();
      });
    });
  });
};

export const getConnectorInfo = worker => {
  let sources = [];
  let sinks = [];

  if (worker) {
    worker.classInfos.forEach(info => {
      const { className, classType } = info;
      const displayClassName = className.split('.').pop();
      if (info.classType === KIND.source) {
        return sources.push({
          name: displayClassName,
          classType,
          className,
        });
      }

      sinks.push({ name: displayClassName, classType, className });
    });
  }

  return [sources, sinks];
};
