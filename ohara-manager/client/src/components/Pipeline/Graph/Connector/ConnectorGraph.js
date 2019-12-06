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
import * as $ from 'jquery';
import * as _ from 'lodash';
import AccountTreeIcon from '@material-ui/icons/AccountTree';
import PlayArrowIcon from '@material-ui/icons/PlayArrow';
import StopIcon from '@material-ui/icons/Stop';
import BuildIcon from '@material-ui/icons/Build';
import ClearIcon from '@material-ui/icons/Clear';

const ConnectorGraph = params => {
  const { value, position, type, icon, graph, paper } = params;

  const start = renderToString(<PlayArrowIcon />);
  const stop = renderToString(<StopIcon />);
  const setting = renderToString(<BuildIcon />);
  const link = renderToString(<AccountTreeIcon />);
  const remove = renderToString(<ClearIcon />);
  let linkLine;

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
  joint.shapes.html.ElementView = joint.dia.ElementView.extend({
    template: [
      '<div class="connector">',
      `<div class="header"><div class="circle">${icon}</div>`,
      `<div class="title-wrapper"><div class="title"></div>`,
      `<div class="type">${
        type === 'Pipeline Only' ? 'Topic' : type
      }</div></div></div>`,
      `<div class="status">`,
      `<span>${'Status'}</span>`,
      `<span>${'Stopped'}</span>`,
      `</div>`,
      `<div class="connectorMenu">`,
      `<Button id="link">${link}</Button>`,
      `<Button id="start">${start}</Button>`,
      `<Button id="stop">${stop}</Button>`,
      `<Button id="setting">${setting}</Button>`,
      `<Button id="remove">${remove}</Button> `,
      `</div>`,
      '</div>',
    ].join(''),

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
        .find('button#remove')
        .on('click', _.bind(this.model.remove, this.model));

      const modelId = this.model.id;
      this.$box.find('button#link').on('mousedown', function() {
        linkLine = new joint.shapes.standard.Link();
        linkLine.source({ id: modelId });
        linkLine.router('manhattan');
        linkLine.attr({ line: { stroke: 'transparent' } });
        linkLine.addTo(graph);
      });

      this.updateBox();
      return this;
    },
    updateBox() {
      // Set the position and dimension of the box so that it covers the JointJS element.
      const bbox = this.getBBox({ useModelGeometry: true });
      const scale = paper.scale();

      this.$box.css({
        transform: 'scale(' + scale.sx + ',' + scale.sy + ')',
        transformOrigin: '0 0',
        width: bbox.width / scale.sx,
        height: bbox.height / scale.sy,
        left: bbox.x,
        top: bbox.y,
      });

      this.$box.find('.title').text(this.model.get('title'));
      this.$box
        .find('.connectorMenu')
        .attr('style', `display:${this.model.get('menuDisplay')};`);

      if (this.paper) {
        this.paper.$document.on('mousemove', function(event) {
          if (linkLine) {
            if (!linkLine.attributes.target.id) {
              linkLine.target({ x: event.pageX - 290, y: event.pageY - 72 });
              linkLine.attr({ line: { stroke: '#333333' } });
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
    position: { x: position.x, y: position.y },
    size: { width: 240, height: 100 },
    title: value,
    menuDisplay: 'none',
  });
};
export default ConnectorGraph;
