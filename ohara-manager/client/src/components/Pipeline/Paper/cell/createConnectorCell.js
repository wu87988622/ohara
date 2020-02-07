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
import PlayArrowIcon from '@material-ui/icons/PlayArrow';
import StopIcon from '@material-ui/icons/Stop';
import BuildIcon from '@material-ui/icons/Build';
import CancelIcon from '@material-ui/icons/Cancel';
import TrendingUpIcon from '@material-ui/icons/TrendingUp';
import FlightTakeoffIcon from '@material-ui/icons/FlightTakeoff';
import FlightLandIcon from '@material-ui/icons/FlightLand';
import WavesIcon from '@material-ui/icons/Waves';
import { renderToString } from 'react-dom/server';
import $ from 'jquery';
import _ from 'lodash';
import * as joint from 'jointjs';

import { KIND, CELL_STATUS } from 'const';

const WIDTH = 240;
const HEIGHT = 100;
const HEIGHT_WITH_METRICS = 160;

const createConnectorCell = options => {
  const {
    id,
    displayName,
    isTemporary = false,
    name,
    kind,
    className,
    position,
    status = CELL_STATUS.stopped,
    paperApi,
    onCellStart,
    onCellStop,
    onCellConfig,
    onCellRemove,
    shouldSkipOnElementAdd,
    metrics = {
      meters: [],
    },
    jarKey,
  } = options;

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
  const configIcon = renderToString(<BuildIcon viewBox="-4 -5 32 32" />);
  const removeIcon = renderToString(<CancelIcon viewBox="-4 -5 32 32" />);

  const displayClassName = className.split('.').pop();
  const iconStatus = getIconStatus(status);

  joint.shapes.html.ElementView = joint.dia.ElementView.extend({
    template: `
      <div class="connector">
        <div class="header">
          <div class="icon ${iconStatus}">${getIcon(kind)}</div>
          <div class="display-name-wrapper">
            <div class="display-name">${displayName}</div>
            <div class="type">${displayClassName}</div>
          </div>
        </div>
        <div class="metrics"></div>
        <div class="status">
          <span class="status-name">Status</span>
          <span class="status-value">${status}</span>
        </div>

        <div class="menu">
          ${
            // Sink cannot create connection form itself to others
            kind !== KIND.sink
              ? `<Button class="link">${linkIcon}</Button>`
              : ''
          }
          <Button class="start">${startIcon}</Button>
          <Button class="stop">${stopIcon}</Button>
          <Button class="config">${configIcon}</Button>
          <Button class="remove">${removeIcon}</Button>
        </div>
    </div>`,

    init() {
      this.listenTo(this.model, 'change', this.updatePosition);
    },
    onRender() {
      const boxMarkup = joint.util.template(this.template)();
      const $box = (this.$box = $(boxMarkup));
      this.listenTo(this.paper, 'scale translate', this.updatePosition);
      $box.appendTo(this.paper.el);

      const $linkButton = $box.find('.link');
      const $startButton = $box.find('.start');
      const $stopButton = $box.find('.stop');
      const $configButton = $box.find('.config');
      const $removeButton = $box.find('.remove');
      const { id } = this.model;
      const cellData = paperApi.getCell(id);

      // Menu actions
      $linkButton.on('click', () => paperApi.addLink(id));
      $startButton.on('click', () => onCellStart(cellData, paperApi));
      $stopButton.on('click', () => onCellStop(cellData, paperApi));
      $configButton.on('click', () => onCellConfig(cellData, paperApi));
      $removeButton.on('click', () => onCellRemove(cellData, paperApi));

      this.updatePosition();

      // TODO: don't show the metrics for now, this is still an issue, and
      //  will be addressed in anther tasks in #3813
      this.toggleMetrics(false);
      return this;
    },
    showMenu() {
      this.$box.find('.menu').show();
    },
    hideMenu() {
      this.$box.find('.menu').hide();
    },
    enableMenu(items = []) {
      const cls = 'is-disabled';
      const $buttons = this.$box.find('.menu > button');

      if (items.length === 0) {
        return $buttons.removeClass(cls);
      }

      $buttons.each((index, button) => {
        if (button.className.includes(items)) {
          $(button).removeClass(cls);
        } else {
          $(button).addClass(cls);
        }
      });
    },
    disableMenu(items = []) {
      const cls = 'is-disabled';
      const $buttons = this.$box.find('.menu > button');

      if (items.length === 0) {
        return $buttons.addClass(cls);
      }

      $buttons.each((index, button) => {
        if (button.className.includes(items)) {
          $(button).addClass(cls);
        } else {
          $(button).removeClass(cls);
        }
      });
    },
    toggleMetrics(isOpen) {
      const { $box, model } = this;

      if (isOpen) {
        $box.find('.metrics').show();

        // Update SVG
        model.resize(WIDTH, HEIGHT_WITH_METRICS);

        // Update HTML
        $box.css({
          width: WIDTH,
          height: HEIGHT_WITH_METRICS,
        });
      } else {
        $box.find('.metrics').hide();

        // Update SVG
        model.resize(WIDTH, HEIGHT);

        // Update HTML
        $box.css({
          width: WIDTH,
          height: HEIGHT,
        });
      }
    },
    updateElement(cellData, newMetrics = { meters: [] }) {
      const $box = this.$box;

      // Metrics
      const meters = _.has(newMetrics, 'meters');
      const displayMetrics = meters.length > 0 ? metrics.meters : metrics;
      const metricsData = getMetrics(displayMetrics);
      $box.find('.metrics').html(metricsData);

      // Status
      const { status } = cellData;
      $box.find('.status-value').text(status);
      this.model.set('status', status);

      // Display name
      $box.find('.display-name').text(displayName);

      // Icon status
      const iconStatus = getIconStatus(status);
      $box
        .find('.icon')
        .removeClass()
        .addClass(`icon ${iconStatus}`);
    },
    updatePosition() {
      // Set the position and dimension of the box so that it covers the JointJS element.
      const { width, height, x, y } = this.getBBox({ useModelGeometry: true });
      const scale = paperApi.getScale();
      const $box = this.$box;

      $box.css({
        transform: 'scale(' + scale.sx + ',' + scale.sy + ')',
        transformOrigin: '0 0',
        width: width / scale.sx,
        height: height / scale.sy,
        left: x,
        top: y,
      });
    },

    // Keeping this handler here since when calling `cell.remove()` somehow
    // triggers this method
    onRemove() {
      this.$box.remove();
    },
  });
  window.$ = $;

  return new joint.shapes.html.Element({
    id: id ? id : undefined, // undefined -> id is controlled by JointJS
    name,
    kind,
    className,
    displayName,
    position,
    status,
    isTemporary,
    size: {
      width: WIDTH,
      height: HEIGHT,
    },
    shouldSkipOnElementAdd,
    jarKey,
  });
};

function getIcon(kind) {
  const sourceIcon = renderToString(<FlightTakeoffIcon color="action" />);
  const sinkIcon = renderToString(<FlightLandIcon color="action" />);
  const streamIcon = renderToString(<WavesIcon color="action" />);
  const { source, sink, stream } = KIND;

  if (kind === source) return sourceIcon;
  if (kind === sink) return sinkIcon;
  if (kind === stream) return streamIcon;
}

function getMetrics(metrics) {
  // Make sure we're getting
  // 1. Same metrics data every time by sorting
  // 2. And removing duplicate items
  // 3. Finally, just pick the values that need to be displayed
  const results = _.map(
    _.sortBy(_.uniqBy(metrics.meters, 'name'), 'name'),
    _.partialRight(_.pick, ['document', 'value']),
  );

  // The user will be able to choose two metrics items with our UI
  // in the future, but for now, we're picking the first two items
  // from the list
  const firstFieldName = _.get(results, '[0].document', '');
  const firstFieldValue = _.get(results, '[0].value', 0);
  const secondFieldName = _.get(results, '[1].document', '');
  const secondFieldValue = _.get(results, '[1].value', 0);

  return `
  <div class="field">
    <span class="field-name">${firstFieldName}</span>
    <span class="field-value">${firstFieldValue.toLocaleString()}</span>
  </div>
  <div class="field">
    <span class="field-name">${secondFieldName}</span>    
    <span class="field-value">${secondFieldValue.toLocaleString()}</span>
  `;
}

function getIconStatus(status) {
  const { stopped, pending, running, failed } = CELL_STATUS;
  const _status = status.toLowerCase();

  if (_status === stopped) return stopped;
  if (_status === pending) return pending;
  if (_status === running) return running;
  if (_status === failed) return failed;

  return stopped;
}

export default createConnectorCell;
