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
import * as joint from 'jointjs';
import $ from 'jquery';
import _ from 'lodash';
import PlayArrowIcon from '@material-ui/icons/PlayArrow';
import StopIcon from '@material-ui/icons/Stop';
import BuildIcon from '@material-ui/icons/Build';
import CancelIcon from '@material-ui/icons/Cancel';
import TrendingUpIcon from '@material-ui/icons/TrendingUp';

import { KIND } from 'const';

const ConnectorGraph = params => {
  const {
    title,
    graph,
    paper,
    isTemporary = false,
    isFetch = false,
    openSettingDialog,
    setData,
    classInfo,
    cellInfo,
    id,
    isMetricsOn = true,
    metrics = {
      meters: [],
    },
    name,
    startConnector,
    stopConnector,
    deleteConnector,
    updatePipeline,
    currentPipeline,
  } = params;

  const { classType, position, icon, displayedClassName } = cellInfo;

  let link;

  joint.shapes.html = {};
  joint.shapes.html.Element = joint.shapes.basic.Rect.extend({
    defaults: joint.util.deepSupplement(
      {
        type: 'html.Element',
        attrs: {
          rect: { stroke: 'none', 'fill-opacity': 0 },
        },
      },
      joint.shapes.basic.Rect.prototype.defaults,
    ),
  });

  const linkIcon = renderToString(<TrendingUpIcon />);
  const startIcon = renderToString(<PlayArrowIcon />);
  const stopIcon = renderToString(<StopIcon />);
  const settingIcon = renderToString(<BuildIcon viewBox="-4 -5 32 32" />);
  const removeIcon = renderToString(<CancelIcon viewBox="-4 -5 32 32" />);

  const isMetricsDisplayed = metrics.length > 0 && isMetricsOn;

  const getMetrics = isMetricsDisplayed => {
    if (!isMetricsDisplayed) return;

    // Make sure we're getting
    // 1. Same metrics data every time by sorting
    // 2. And removing duplicate items
    // 3. Finally, just pick the values that need to be displayed
    const results = _.map(
      _.sortBy(_.uniqBy(metrics.meters, 'name'), 'name'),
      _.partialRight(_.pick, ['document', 'value']),
    );
    return results;
  };

  const displayMetrics = getMetrics(isMetricsDisplayed);

  // The user will be able to choose two metrics items with our UI
  // in the future, but for now, we're picking the first two items
  // from the list
  const firstFieldName = _.get(displayMetrics, '[0].document', '');
  const firstFieldValue = _.get(displayMetrics, '[0].value', 0);
  const secondFieldName = _.get(displayMetrics, '[1].document', '');
  const secondFieldValue = _.get(displayMetrics, '[1].value', 0);

  joint.shapes.html.ElementView = joint.dia.ElementView.extend({
    template: `
      <div class="connector">
        <div class="header">
          <div class="icon">${icon}</div>
          <div class="title-wrapper">
            <div class="title"></div>
              <div class="type">${displayedClassName}</div>
            </div>
          </div>

          ${
            isMetricsDisplayed
              ? `<div class="metrics">
            <div class="field">
              <span class="field-name">${firstFieldName}</span>
              <span class="field-value">${firstFieldValue.toLocaleString()}</span>
            </div>
            <div class="field">
            <span class="field-name">${secondFieldName}</span>
            <span class="field-value">${secondFieldValue.toLocaleString()}</span>
          </div>
          </div>`
              : ''
          }
         
        <div class="status">
          <span>${'Status'}</span>
          <span>${'Stopped'}</span>
        </div>

        <div class="connector-menu">
          ${
            // Sink cannot create connection form itself to others
            classType !== KIND.sink
              ? `<Button id="connector-link">${linkIcon}</Button>`
              : ''
          }
          <Button id="connector-start">${startIcon}</Button>
          <Button id="connector-stop">${stopIcon}</Button>
          <Button id="connector-setting">${settingIcon}</Button>
          <Button id="connector-remove">${removeIcon}</Button>
        </div>
    </div>`,

    init() {
      this.listenTo(this.model, 'change', this.updateBox);
    },
    onRender() {
      if (this.$box) this.$box.remove();

      const boxMarkup = joint.util.template(this.template)();
      const $box = (this.$box = $(boxMarkup));
      this.listenTo(this.paper, 'scale translate', this.updateBox);

      $box.appendTo(this.paper.el);

      const modelId = this.model.id;
      this.$box.find('#connector-link').on('mousedown', function() {
        link = new joint.shapes.standard.Link();
        link.source({ id: modelId });

        // The link doesn't show up in the right position, set it to
        // `transparent` and reset it back in the mousemove event
        link.attr({ line: { stroke: 'transparent' } });
        link.addTo(graph.current);
      });

      this.$box.find('#connector-setting').on('mousedown', function() {
        openSettingDialog();
        setData({
          title: `Editing the settings for ${title} ${displayedClassName}`,
          classInfo,
        });
      });

      this.$box.find('#connector-start').on('mousedown', () => {
        startConnector(name);
      });

      this.$box.find('#connector-stop').on('mousedown', () => {
        stopConnector(name);
      });

      this.$box
        .find('#connector-remove')
        .on('click', _.bind(this.model.remove, this.model));

      this.$box.find('#connector-remove').on('mousedown', async () => {
        await deleteConnector(name);

        const removedEndpoints = currentPipeline.endpoints.filter(
          endpoint => endpoint.name !== name,
        );

        await updatePipeline({
          name: currentPipeline.name,
          endpoints: removedEndpoints,
          tags: graph.current.toJSON(),
        });
      });

      this.updateBox();
      return this;
    },
    updateBox() {
      // Set the position and dimension of the box so that it covers the JointJS element.
      const bBox = this.getBBox({ useModelGeometry: true });
      const scale = paper.current.scale();

      this.$box.css({
        transform: 'scale(' + scale.sx + ',' + scale.sy + ')',
        transformOrigin: '0 0',
        width: bBox.width / scale.sx,
        height: bBox.height / scale.sy,
        left: bBox.x,
        top: bBox.y,
      });

      this.$box.find('.title').text(this.model.get('title'));
      this.$box
        .find('.connector-menu')
        .attr('style', `display:${this.model.get('menuDisplay')};`);

      if (this.paper) {
        this.paper.$document.on('mousemove', function(event) {
          if (link) {
            if (!link.get('target').id) {
              const localPoint = paper.current.paperToLocalPoint(
                paper.current.translate(),
              );

              // 290: AppBar and Navigator width
              // 72: Toolbar height
              link.target({
                x: (event.pageX - 290) / scale.sx + localPoint.x,
                y: (event.pageY - 72) / scale.sy + localPoint.y,
              });

              link.attr({
                // prevent the link from clicking by users, the `root` here is the
                // SVG container element of the link
                root: { style: 'pointer-events: none' },
                line: { stroke: '#9e9e9e' },
              });
            }
          }
        });
      }
    },
    onRemove() {
      this.$box.remove();
    },
  });

  return new joint.shapes.html.Element({
    id: id ? id : undefined, // undefined -> id is controlled by JointJS
    size: { width: 240, height: isMetricsDisplayed ? 160 : 100 },
    menuDisplay: 'none',
    position,
    title,
    classType,
    isTemporary,
    isFetch,
    params: _.omit(params, [
      'graph',
      'paper',
      'openSettingDialog',
      'setData',
      'classInfo',
      'startConnector',
      'stopConnector',
      'deleteConnector',
    ]),
  });
};
export default ConnectorGraph;
