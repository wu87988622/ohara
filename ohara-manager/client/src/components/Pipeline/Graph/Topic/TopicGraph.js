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
import { PrivateTopicIcon, PublicTopicIcon } from 'components/common/Icon';
import AccountTreeIcon from '@material-ui/icons/AccountTree';
import BuildIcon from '@material-ui/icons/Build';
import ClearIcon from '@material-ui/icons/Clear';

const TopicGraph = params => {
  const { value, position, zIndex, graph, type } = params;

  const privateIcon = renderToString(
    <PrivateTopicIcon width={56} height={56} />,
  );
  const pulibcIcon = renderToString(<PublicTopicIcon width={56} height={56} />);
  const setting = renderToString(<BuildIcon />);
  const link = renderToString(<AccountTreeIcon />);
  const remove = renderToString(<ClearIcon />);
  let linkLine;
  const height = type !== 'public' ? 0 : 22;
  const topicValue = type === 'public' ? value : '';

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
      '<div class="topic">',
      `${type === 'public' ? pulibcIcon : privateIcon}`,
      `<div class="title"></div>`,
      `<div class="topicMenu">`,
      `<Button id="link">${link}</Button>`,
      `<Button id="setting">${setting}</Button>`,
      `<Button id="remove">${remove}</Button> `,
      `</div>`,
      '</div>',
    ].join(''),
    initialize() {
      _.bindAll(this, 'updateBox');
      joint.dia.ElementView.prototype.initialize.apply(this, arguments);

      this.$box = $(_.template(this.template)());

      // Update the box position whenever the underlying model changes.
      this.model.on('change', this.updateBox, this);
      // Remove the box when the model gets removed from the graph.
      this.model.on('remove', this.removeBox, this);

      const modelId = this.model.id;

      this.$box
        .find('button#remove')
        .on('click', _.bind(this.model.remove, this.model));

      //Click the connect button to generate SVG's link object,
      //starting from its own box,
      //but we don't want to let users see it at the beginning,
      //so adjust the attributes and display it later
      this.$box.find('button#link').on('mousedown', function() {
        linkLine = new joint.shapes.standard.Link();
        linkLine.source({ id: modelId });
        linkLine.router('manhattan');
        linkLine.attr({ line: { stroke: 'transparent' } });
        linkLine.addTo(graph);
      });

      this.updateBox();
    },
    render() {
      joint.dia.ElementView.prototype.render.apply(this, arguments);
      this.paper.$el.prepend(this.$box);
      this.updateBox();
      return this;
    },
    updateBox() {
      // Set the position and dimension of the box so that it covers the JointJS element.
      var bbox = this.model.getBBox();

      // Example of updating the HTML with a data stored in the cell model.
      this.$box.css({
        width: bbox.width,
        height: bbox.height,
        left: bbox.x,
        top: bbox.y,
        transform: 'rotate(' + (this.model.get('angle') || 0) + 'deg)',
        'z-index': zIndex,
      });
      this.$box.find('.title').text(this.model.get('title'));
      this.$box
        .find('.topicMenu')
        .attr('style', `display:${this.model.get('menuDisplay')};`);
      if (this.paper) {
        this.paper.$document.on('mousemove', function(evt) {
          if (linkLine) {
            if (!linkLine.attributes.target.id) {
              linkLine.target({ x: evt.pageX - 290, y: evt.pageY - 72 });
              linkLine.attr({ line: { stroke: '#333333' } });
            }
          }
        });
      }
    },
    removeBox() {
      this.$box.remove();
    },
  });

  let el = new joint.shapes.html.Element({
    position: { x: position.x, y: position.y },
    size: { width: 56, height: 56 + height },
    title: topicValue,
    menuDisplay: 'none',
  });
  return el;
};
export default TopicGraph;
