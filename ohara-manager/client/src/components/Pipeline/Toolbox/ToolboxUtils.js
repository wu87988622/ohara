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
import { AddSharedTopicIcon } from 'components/common/Icon';
import { getPipelineOnlyTopicDisplayNames } from '../PipelineUtils';
import * as generate from 'utils/generate';

export const removeTemporaryCell = paperApi => {
  // Remove temporary cells
  paperApi
    .getCells()
    .filter(cell => cell.isTemporary)
    .forEach(cell => paperApi.removeElement(cell.id));
};

export const checkUniqueName = (name, paperApi) => {
  const isUnique = paperApi
    .getCells()
    .filter(cell => cell.cellType === 'html.Element')
    .every(element => element.name !== name);

  return Boolean(isUnique);
};

export const createToolboxList = params => {
  const {
    connectors,
    sourceGraph,
    topicGraph,
    streamGraph,
    sinkGraph,
    searchResults,
    paperApi,
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
      const { $box, model } = this;

      $box.find('.display-name').text(model.get('name'));
      $box.find('.icon').html(model.get('icon'));

      const isShared = model.get('isShared');
      if (isShared !== undefined) {
        $box.addClass(isShared ? 'shared' : 'pipeline-only');
      }

      if (model.get('isDisabled') !== undefined) {
        model.get('isDisabled')
          ? $box.addClass('is-disabled')
          : $box.removeClass('is-disabled');
      }
    },
  });

  const sourceIcon = renderToString(<FlightTakeoffIcon color="action" />);
  const sinkIcon = renderToString(<FlightLandIcon color="action" />);
  const streamIcon = renderToString(<WavesIcon color="action" />);
  const AddPipelineOnlyTopic = renderToString(<StorageIcon color="action" />);

  // Custom icon, so we need to pass some props...
  const AddSharedTopic = renderToString(
    <AddSharedTopicIcon className="shared-topic" width={23} height={22} />,
  );

  const displaySources = isNull(searchResults)
    ? sources
    : searchResults.sources;

  // Create JointJS elements and add them to the graph as usual.
  displaySources.forEach((source, index) => {
    sourceGraph.current.addCell(
      new joint.shapes.html.Element({
        position: { x: 10, y: index * 38 },
        size: { width: 272 - 8 * 2, height: 38 },
        name: source.name,
        kind: source.kind,
        icon: sourceIcon,
        className: source.className,
      }),
    );
  });

  const displayTopics = isNull(searchResults) ? topics : searchResults.topics;

  displayTopics.forEach((topic, index) => {
    // Shared topic can only be added into the Paper once
    const isDisabled = paperApi
      .getCells('topic')
      .some(t => t.name === topic.name);

    topicGraph.current.addCell(
      new joint.shapes.html.Element({
        position: { x: 10, y: index * 38 },
        size: { width: 272 - 8 * 2, height: 38 },
        name: topic.name,
        kind: topic.kind,
        className: topic.className,
        icon: topic.isShared ? AddSharedTopic : AddPipelineOnlyTopic,
        isShared: topic.isShared,
        isDisabled,
      }),
    );
  });

  const displayStreams = isNull(searchResults)
    ? streams
    : searchResults.streams;

  displayStreams.forEach((stream, index) => {
    streamGraph.current.addCell(
      new joint.shapes.html.Element({
        position: { x: 10, y: index * 38 },
        size: { width: 272 - 8 * 2, height: 38 },
        name: stream.name,
        kind: stream.kind,
        icon: streamIcon,
        className: stream.className,
        jarKey: stream.jarKey,
      }),
    );
  });

  const displaySinks = isNull(searchResults) ? sinks : searchResults.sinks;

  displaySinks.forEach((sink, index) => {
    sinkGraph.current.addCell(
      new joint.shapes.html.Element({
        position: { x: 10, y: index * 38 },
        size: { width: 272 - 8 * 2, height: 38 },
        name: sink.name,
        kind: sink.kind,
        icon: sinkIcon,
        className: sink.className,
      }),
    );
  });
};

export const enableDragAndDrop = params => {
  const {
    toolPapers,
    setCellInfo,
    setIsOpen: openAddConnectorDialog,
    paperApi,
    showMessage,
  } = params;

  toolPapers.forEach(toolPaper => {
    // Add "hover" state in items, I cannot figure out how to do
    // this when initializing the HTML elements...
    toolPaper.on('cell:mouseenter', cellView => {
      if (cellView.model.get('isDisabled')) return;
      cellView.$box.css('backgroundColor', 'rgba(0, 0, 0, 0.08)');
    });

    toolPaper.on('cell:mouseleave', cellView => {
      if (cellView.model.get('isDisabled')) return;
      cellView.$box.css('backgroundColor', 'transparent');
    });

    // Create "flying papers", which enable drag and drop feature
    toolPaper.on('cell:pointerdown', (cellView, event, x, y) => {
      if (cellView.model.get('isDisabled')) return;

      const cellKind = cellView.model.get('kind');
      $('#paper').append(
        `<div data-testid="flying-element" class="flying-paper flying-${cellKind}"></div>`,
      );

      const flyingGraph = new joint.dia.Graph();
      new joint.dia.Paper({
        el: $('.flying-paper'),
        width: cellKind === KIND.topic ? 60 : 160,
        height: 60,
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

      $('.flying-paper').offset({
        left: event.pageX - offset.x,
        top: event.pageY - offset.y,
      });

      $('#paper').on('mousemove.fly', event => {
        // Prevent the flying-paper from interfering by the Toolbox.
        // We will set this back after the drag-and-drop action is finished
        $('.toolbox').css('pointer-events', 'none');

        $('.flying-paper').offset({
          left: event.pageX - offset.x,
          top: event.pageY - offset.y,
        });
      });

      $('#paper').on('mouseup.fly', event => {
        const x = event.pageX;
        const y = event.pageY;
        const paperBbox = paperApi.getBbox();
        const $toolbox = $('.toolbox');

        const isInsideToolbox =
          x > $toolbox.offset().left &&
          x < $toolbox.offset().left + $toolbox.width() &&
          y > $toolbox.offset().top &&
          y < $toolbox.offset().top + $toolbox.height();

        const isInsidePaper =
          x > paperBbox.offsetLeft &&
          x < paperBbox.offsetLeft + paperBbox.width &&
          y > paperBbox.offsetTop &&
          y < paperBbox.offsetTop + paperBbox.height;

        // Dropped over paper ?
        if (isInsidePaper && !isInsideToolbox) {
          const localPoint = paperApi.getLocalPoint();
          const scale = paperApi.getScale();
          const newX =
            (x - paperBbox.offsetLeft - offset.x) / scale.sx + localPoint.x;
          const newY =
            (y - paperBbox.offsetTop - offset.y) / scale.sy + localPoint.y;
          const {
            kind,
            className,
            name,
            jarKey,
            isShared,
          } = cellView.model.attributes;
          const isTopic = kind === KIND.topic;

          // These info will be used when creating a cell
          const params = {
            name,
            position: { x: newX, y: newY },
            kind,
            className,
            jarKey: jarKey || null,
            isShared,
          };

          setCellInfo(prevState => ({
            ...prevState,
            ...params,
          }));

          if (isTopic) {
            if (isShared) {
              if (!checkUniqueName(name, paperApi)) {
                showMessage(
                  `The name "${name}" is already taken in this pipeline, please use a different name!`,
                );
              } else {
                paperApi.addElement({
                  ...params,
                  displayName: name,
                });
              }
            } else {
              paperApi.addElement({
                ...params,
                name: generate.serviceName(),
                displayName: getPipelineOnlyTopicDisplayNames(
                  paperApi.getCells('topic'),
                ),
              });
            }
          } else {
            openAddConnectorDialog(true);

            // A temporary cell which gives users a better idea of
            // where the graph will be added at. It will be removed
            // once the real graph is added
            paperApi.addElement(
              {
                ...params,
                isTemporary: true,
                displayName: 'newgraph',
              },
              { skipGraphEvents: true },
            );
          }
        }
        // Clean up
        $('#paper')
          .off('mousemove.fly')
          .off('mouseup.fly');
        flyingShape.remove();
        $('.flying-paper').remove();
        $('.toolbox').css('pointer-events', 'auto');
      });
    });
  });
};

export function getConnectorInfo(worker) {
  let sources = [];
  let sinks = [];

  if (worker && worker.classInfos) {
    worker.classInfos.forEach(info => {
      const { className, settingDefinitions: defs } = info;
      const kind = defs.find(def => def.key === 'kind')?.defaultValue;
      const displayClassName = className.split('.').pop();
      if (kind === KIND.sink) {
        return sinks.push({
          name: displayClassName,
          kind,
          className,
        });
      }

      sources.push({ name: displayClassName, kind, className });
    });
  }

  return [sources, sinks];
}
