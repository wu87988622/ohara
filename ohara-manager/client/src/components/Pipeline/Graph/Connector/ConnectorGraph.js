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

const ConnectorGraph = params => {
  const {
    title,
    graph,
    paper,
    isTemporary = false,
    openSettingDialog,
    setData,
    classInfo,
    cellInfo,
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

  joint.shapes.html.ElementView = joint.dia.ElementView.extend({
    template: `
      <div class="connector">
        <div class="header">
          <div class="circle">${icon}</div>
          <div class="title-wrapper">
            <div class="title"></div>
              <div class="type">${displayedClassName}</div>
            </div>
          </div>
        <div class="status">
          <span>${'Status'}</span>
          <span>${'Stopped'}</span>
        </div>
        <div class="connector-menu">
          ${
            // Sink cannot create connection form itself to others
            classType !== 'sink'
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

      // Bind remove event to our custom icon
      this.$box
        .find('#connector-remove')
        .on('click', _.bind(this.model.remove, this.model));

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
    size: { width: 240, height: 100 },
    menuDisplay: 'none',
    position,
    title,
    classType,
    isTemporary,
  });
};
export default ConnectorGraph;
