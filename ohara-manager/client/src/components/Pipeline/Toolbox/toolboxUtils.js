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
import { isNull, bindAll, template } from 'lodash';
import FlightTakeoffIcon from '@material-ui/icons/FlightTakeoff';
import FlightLandIcon from '@material-ui/icons/FlightLand';
import StorageIcon from '@material-ui/icons/Storage';
import WavesIcon from '@material-ui/icons/Waves';
import * as $ from 'jquery';
import * as joint from 'jointjs';

import { AddPublicTopicIcon } from 'components/common/Icon';

export const createToolboxList = ({
  sources,
  sinks,
  streams,
  topics,
  sourceGraph,
  topicGraph,
  streamGraph,
  sinkGraph,
  searchResults,
}) => {
  const sourceIcon = renderToString(<FlightTakeoffIcon color="action" />);
  const sinkIcon = renderToString(<FlightLandIcon color="action" />);
  const streamIcon = renderToString(<WavesIcon color="action" />);
  const AddPrivateTopic = renderToString(<StorageIcon color="action" />);
  const AddPublicTopic = renderToString(
    <AddPublicTopicIcon className="public-topic" width={23} height={22} />,
  );

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
    template: [
      `<div class="item">
      <span class="icon"></span>
      <span class="display-name"></span>
      </div>`,
    ],

    initialize() {
      bindAll(this, 'updateBox');
      joint.dia.ElementView.prototype.initialize.apply(this, arguments);
      this.$box = $(template(this.template)());
      // Update the box position whenever the underlying model changes.
      this.model.on('change', this.updateBox, this);
      this.updateBox();
    },
    render() {
      joint.dia.ElementView.prototype.render.apply(this, arguments);
      this.paper.$el.append(this.$box);
      this.updateBox();
      return this;
    },

    updateBox() {
      // Updating the HTML with a data stored in the cell model.
      this.$box.find('.display-name').text(this.model.get('displayName'));
      this.$box.find('.icon').html(this.model.get('icon'));
    },
  });

  const displaySources = isNull(searchResults)
    ? sources
    : searchResults.sources;

  // Create JointJS elements and add them to the graph as usual.
  displaySources.forEach((source, index) => {
    sourceGraph.current.addCell(
      new joint.shapes.html.Element({
        position: { x: 10, y: index * 44 },
        size: { width: 272 - 8 * 2, height: 44 },
        displayName: source.displayName,
        classType: source.classType,
        icon: sourceIcon,
      }),
    );
  });

  const displayTopics = isNull(searchResults) ? topics : searchResults.topics;

  displayTopics.forEach((topic, index) => {
    topicGraph.current.addCell(
      new joint.shapes.html.Element({
        position: { x: 10, y: index * 44 },
        size: { width: 272 - 8 * 2, height: 44 },
        displayName: topic.displayName,
        classType: topic.classType,
        icon: index === 0 ? AddPrivateTopic : AddPublicTopic,
      }),
    );
  });

  const displayStreams = isNull(searchResults)
    ? streams
    : searchResults.streams;

  displayStreams.forEach((stream, index) => {
    streamGraph.current.addCell(
      new joint.shapes.html.Element({
        position: { x: 10, y: index * 44 },
        size: { width: 272 - 8 * 2, height: 44 },
        displayName: stream.displayName,
        classType: stream.classType,
        icon: streamIcon,
      }),
    );
  });

  const displaySinks = isNull(searchResults) ? sinks : searchResults.sinks;

  displaySinks.forEach((sink, index) => {
    sinkGraph.current.addCell(
      new joint.shapes.html.Element({
        position: { x: 10, y: index * 44 },
        size: { width: 272 - 8 * 2, height: 44 },
        displayName: sink.displayName,
        classType: sink.classType,
        icon: sinkIcon,
      }),
    );
  });
};

export const enableDragAndDrop = ({
  toolPapers,
  paper,
  setGraphType,
  setPosition,
  setIsOpen: openAddConnectorDialog,
}) => {
  toolPapers.forEach(toolPaper => {
    // Add "hover" state in items, I cannot figure out how to do
    // this when initializing the HTML elements...
    toolPaper.on('cell:mouseenter', function(cellView) {
      cellView.$box.css('backgroundColor', 'rgba(0, 0, 0, 0.08)');
    });

    toolPaper.on('cell:mouseleave', function(cellView) {
      cellView.$box.css('backgroundColor', 'transparent');
    });

    // Create "flying papers", which enable drag and drop feature
    toolPaper.on('cell:pointerdown', function(cellView, e, x, y) {
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
      const pos = cellView.model.position();
      const offset = {
        x: x - pos.x,
        y: y - pos.y,
      };
      setGraphType(cellView.model.get('classType'));

      flyingShape.position(0, 0);
      flyingGraph.addCell(flyingShape);

      $('#flying-paper').offset({
        left: e.pageX - offset.x,
        top: e.pageY - offset.y,
      });

      function isInsidePaper() {
        const target = paper.$el.offset();
        const x = e.pageX;
        const y = e.pageY;

        return (
          x > target.left &&
          x < target.left + paper.$el.width() &&
          y > target.top &&
          y < target.top + paper.$el.height()
        );
      }

      $('#paper').on('mousemove.fly', e => {
        $('#flying-paper').offset({
          left: e.pageX - offset.x,
          top: e.pageY - offset.y,
        });
      });

      $('#paper').on('mouseup.fly', e => {
        const x = e.pageX;
        const y = e.pageY;
        const target = paper.$el.offset();

        // Dropped over paper ?
        if (isInsidePaper()) {
          openAddConnectorDialog(true);
          setPosition({
            x: x - target.left - offset.x,
            y: y - target.top - offset.y,
          });
        }

        $('#paper')
          .off('mousemove.fly')
          .off('mouseup.fly');
        flyingShape.remove();

        $('#flying-paper').remove();
      });
    });
  });
};
