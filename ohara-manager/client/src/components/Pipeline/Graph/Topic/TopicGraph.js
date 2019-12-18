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
import BuildIcon from '@material-ui/icons/Build';
import CancelIcon from '@material-ui/icons/Cancel';
import TrendingUpIcon from '@material-ui/icons/TrendingUp';

const TopicGraph = params => {
  const { title, paper, graph, isTemporary = false, cellInfo } = params;
  const { classType, className, position } = cellInfo;

  let link;
  const height = className === 'publicTopic' ? 22 : 0;
  const topicTitle = className === 'publicTopic' ? title : '';

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

  const privateIcon = renderToString(
    <PrivateTopicIcon width={56} height={56} />,
  );
  const publicIcon = renderToString(<PublicTopicIcon width={56} height={56} />);
  const linkIcon = renderToString(<TrendingUpIcon />);
  const settingIcon = renderToString(<BuildIcon viewBox="-4 -5 32 32" />);
  const removeIcon = renderToString(<CancelIcon viewBox="-4 -5 32 32" />);

  joint.shapes.html.ElementView = joint.dia.ElementView.extend({
    template: [
      '<div class="topic">',
      `${className === 'publicTopic' ? publicIcon : privateIcon}`,
      `<div class="title"></div>`,
      `<div class="topicMenu">`,
      `<Button id="link">${linkIcon}</Button>`,
      `<Button id="setting">${settingIcon}</Button>`,
      `<Button id="remove">${removeIcon}</Button> `,
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
        link = new joint.shapes.standard.Link();
        link.source({ id: modelId });
        link.attr({ line: { stroke: 'transparent' } });
        link.addTo(graph);
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
        .find('.topicMenu')
        .attr('style', `display:${this.model.get('menuDisplay')};`);
      if (this.paper) {
        this.paper.$document.on('mousemove', function(event) {
          if (link) {
            if (!link.get('target').id) {
              const localPoint = paper.paperToLocalPoint(paper.translate());

              link.target({
                x: (event.pageX - 290) / scale.sx + localPoint.x,
                y: (event.pageY - 72) / scale.sy + localPoint.y,
              });
              link.attr({ line: { stroke: '#9e9e9e' } });
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
    size: { width: 56, height: 56 + height },
    position,
    title: topicTitle,
    menuDisplay: 'none',
    classType,
    isTemporary,
  });
};
export default TopicGraph;
