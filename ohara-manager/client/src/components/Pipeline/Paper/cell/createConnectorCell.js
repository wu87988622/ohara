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

const createConnectorCell = options => {
  const {
    id,
    name,
    kind,
    className,
    displayName,
    position,
    status = CELL_STATUS.stopped,
    metrics = {
      meters: [],
    },
    paperApi,
    jarKey,
    isMetricsOn,
    shouldSkipOnElementAdd = false,
    isTemporary = false,
    isSelected = false,
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
      <div class="connector" data-testid=${id}>
        <div class="header">
          <div class="icon ${iconStatus}">${getIcon(kind)}</div>
          <div class="display-name-wrapper">
            <div class="display-name">${displayName}</div>
            <div class="type">${displayClassName}</div>
          </div>
        </div>
        <div class="body">
          <div class="metrics">Loading metrics...</div>
          <div class="status">
            <span class="status-name">Status</span>
            <span class="status-value">${status}</span>
          </div>

          <div class="menu">
            <div class="menu-inner">
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
            <div>
          </div>
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

      // Binding events for paper
      $box.find('.link').on(
        'click',
        function(event) {
          this.notify('element:link:button:pointerclick', event);
        }.bind(this),
      );
      $box.find('.start').on(
        'click',
        function(event) {
          this.notify('element:start:button:pointerclick', event);
        }.bind(this),
      );
      $box.find('.stop').on(
        'click',
        function(event) {
          this.notify('element:stop:button:pointerclick', event);
        }.bind(this),
      );
      $box.find('.config').on(
        'click',
        function(event) {
          this.notify('element:config:button:pointerclick', event);
        }.bind(this),
      );
      $box.find('.remove').on(
        'click',
        function(event) {
          this.notify('element:remove:button:pointerclick', event);
        }.bind(this),
      );

      this.toggleMetrics(isMetricsOn);
      this.updatePosition();
      return this;
    },
    showElement(selector) {
      this.$box.find(`.${selector}`).show();
      return this;
    },
    hideElement(selector) {
      this.$box.find(`.${selector}`).hide();
      return this;
    },
    enableMenu(items = []) {
      const cls = 'is-disabled';
      const $buttons = this.$box.find('.menu button');

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
      return this;
    },
    disableMenu(items = []) {
      const cls = 'is-disabled';
      const $buttons = this.$box.find('.menu button');

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
      return this;
    },
    setIsSelected(isSelected) {
      this.model.set('isSelected', isSelected);
      return this;
    },
    toggleMetrics(isOpen) {
      const { $box, model } = this;
      if (isOpen) {
        $box.find('.metrics').show();
        $box.find('.status').hide();
        model.set('isMetricsOn', true);
      } else {
        $box.find('.metrics').hide();
        $box.find('.status').show();
        model.set('isMetricsOn', false);
      }
      return this;
    },
    updateMeters(newMetrics) {
      const meters = _.get(newMetrics, 'meters');
      const displayMetrics = meters.length > 0 ? meters : metrics;
      const metricsData = getMetrics(displayMetrics);
      this.$box.find('.metrics').html(metricsData);
      return this;
    },
    updateElement(cellData) {
      const { $box, model } = this;

      // Status
      const { status } = cellData;
      $box.find('.status-value').text(status);
      model.set('status', status);

      // Display name
      $box.find('.display-name').text(displayName);

      // Icon status
      const iconStatus = getIconStatus(status);
      $box
        .find('.icon')
        .removeClass()
        .addClass(`icon ${iconStatus}`);
      return this;
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
      return this;
    },

    // Keeping this handler here since when calling `cell.remove()` somehow
    // triggers this method
    onRemove() {
      this.$box.remove();
    },
  });

  return new joint.shapes.html.Element({
    id,
    name,
    kind,
    className,
    displayName,
    position,
    status,
    isTemporary,
    isMetricsOn,
    shouldSkipOnElementAdd,
    jarKey,
    isSelected,
    size: {
      width: 240,
      height: 100,
    },
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

function getMetrics(meters) {
  // Make sure we're getting
  // 1. Same metrics data every time by sorting
  // 2. And removing duplicate items
  // 3. Finally, just pick the values that need to be displayed
  const results = _.map(
    _.sortBy(_.uniqBy(meters, 'name'), 'name'),
    _.partialRight(_.pick, ['document', 'value']),
  );

  // The user will be able to choose a metrics item with our UI
  // in the future, but for now, we're displaying the first item
  // from the list
  const firstFieldName = _.get(
    results,
    '[0].document',
    'No metrics data available',
  );
  const defaultValue = firstFieldName === 'No metrics data available' ? '' : 0;
  const firstFieldValue = _.get(results, '[0].value', defaultValue);

  return `
  <div class="field">
    <span class="field-name">${firstFieldName}</span>
    <span class="field-value">${firstFieldValue.toLocaleString()}</span>
  </div>
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
