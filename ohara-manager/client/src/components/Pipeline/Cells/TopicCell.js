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
import CancelIcon from '@material-ui/icons/Cancel';
import TrendingUpIcon from '@material-ui/icons/TrendingUp';
import { renderToString } from 'react-dom/server';
import * as joint from 'jointjs';
import _ from 'lodash';
import $ from 'jquery';

import { PrivateTopicIcon, PublicTopicIcon } from 'components/common/Icon';
import { CELL_STATUS } from 'const';

const TopicCell = options => {
  const {
    id,
    name,
    displayName,
    classType,
    className,
    position,
    paperApi,
    onCellRemove,
    status = CELL_STATUS.stopped,
  } = options;

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

  const privateIcon = renderToString(
    <PrivateTopicIcon width={56} height={56} />,
  );
  const publicIcon = renderToString(<PublicTopicIcon width={56} height={56} />);
  const linkIcon = renderToString(<TrendingUpIcon />);
  const removeIcon = renderToString(<CancelIcon viewBox="-4 -5 32 32" />);

  joint.shapes.html.ElementView = joint.dia.ElementView.extend({
    template: `
      <div class="topic">
        ${className === 'publicTopic' ? publicIcon : privateIcon}
        <div class="display-name">${displayName}</div>
        <div class="topic-menu">
          <Button class="topic-link">${linkIcon}</Button>
          <Button class="topic-remove">${removeIcon}</Button> 
        </div>
      </div>`,
    init() {
      this.listenTo(this.model, 'change', this.updateBox);
    },
    onRender() {
      const boxMarkup = joint.util.template(this.template)();
      const $box = (this.$box = $(boxMarkup));
      this.listenTo(this.paper, 'scale translate', this.updateBox);
      $box.appendTo(this.paper.el);

      const $linkButton = this.$box.find('.topic-link');
      const $removeButton = this.$box.find('.topic-remove');

      const id = this.model.id;
      const name = this.attributes.name;

      $linkButton.on('mousedown', () => {
        paperApi.addCell(id);
      });

      $removeButton.on('click', () => {
        if (_.isFunction(onCellRemove)) onCellRemove(id, name);
        this.$box.remove();
      });

      this.updateBox();
      return this;
    },
    updateBox() {
      // Set the position and dimension of the box so that it covers the JointJS element.
      const bBox = this.getBBox({ useModelGeometry: true });
      const scale = paperApi.scale();

      this.$box.css({
        transform: 'scale(' + scale.sx + ',' + scale.sy + ')',
        transformOrigin: '0 0',
        width: bBox.width / scale.sx,
        height: bBox.height / scale.sy,
        left: bBox.x,
        top: bBox.y,
      });

      const displayValue = this.model.get('isMenuDisplayed') ? 'block' : 'none';
      this.$box.find('.display-name').text(this.model.get('displayName'));
      this.$box.find('.topic-menu').attr('style', `display: ${displayValue};`);

      if (this.paper) {
        this.paper.$document.on('mousemove', function(event) {
          if (link) {
            if (!link.get('target').id) {
              const localPoint = paperApi.getLocalPoint();

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
    name,
    classType,
    className,
    displayName,
    position,
    status,
    size: { width: 56, height: 76 },
    isMenuDisplayed: false,
  });
};
export default TopicCell;
