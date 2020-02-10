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
import $ from 'jquery';

import { SharedTopicIcon, PipelineOnlyTopicIcon } from 'components/common/Icon';
import { CELL_STATUS } from 'const';

const createTopicCell = options => {
  const {
    id,
    name,
    displayName,
    kind,
    className,
    position,
    paperApi,
    status = CELL_STATUS.stopped,
    isShared,
    shouldSkipOnElementAdd = false,
    statusColors,
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

  const pipelineOnlyIcon = renderToString(
    <PipelineOnlyTopicIcon
      statusColor={statusColors[getIconStatus(status)]}
      width={56}
      height={56}
    />,
  );
  const sharedIcon = renderToString(
    <SharedTopicIcon
      statusColor={statusColors[getIconStatus(status)]}
      width={56}
      height={56}
    />,
  );
  const linkIcon = renderToString(<TrendingUpIcon />);
  const removeIcon = renderToString(<CancelIcon viewBox="-4 -5 32 32" />);

  joint.shapes.html.ElementView = joint.dia.ElementView.extend({
    template: `
      <div class="topic">
        ${isShared ? sharedIcon : pipelineOnlyIcon}
        <div class="display-name">${displayName}</div>
        <div class="menu">
          <Button class="link">${linkIcon}</Button>
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

      $box.find('.link').on(
        'click',
        function(event) {
          this.notify('element:link:button:pointerclick', event);
        }.bind(this),
      );

      $box.find('.remove').on(
        'click',
        function(event) {
          this.notify('element:remove:button:pointerclick', event);
        }.bind(this),
      );

      this.updatePosition();
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
    updateElement(cellData) {
      const { status } = cellData;
      const $box = this.$box;
      $box.find('.display-name').text(displayName);
      $box.find('.topic-status').attr('fill', statusColors[status]);

      this.model.set('status', status);
    },
    updatePosition() {
      // Set the position and dimension of the box so that it covers the JointJS element.
      const bBox = this.getBBox({ useModelGeometry: true });
      const scale = paperApi.getScale();

      this.$box.css({
        transform: 'scale(' + scale.sx + ',' + scale.sy + ')',
        transformOrigin: '0 0',
        width: bBox.width / scale.sx,
        height: bBox.height / scale.sy,
        left: bBox.x,
        top: bBox.y,
      });
    },
    onRemove() {
      this.$box.remove();
    },
  });

  return new joint.shapes.html.Element({
    id: id ? id : undefined, // undefined -> id is controlled by JointJS
    name,
    kind,
    className,
    displayName,
    position,
    status,
    isShared,
    size: { width: 56, height: 76 },
    isMenuDisplayed: false,
    shouldSkipOnElementAdd,
  });
};

function getIconStatus(status) {
  const { stopped, pending, running, failed } = CELL_STATUS;
  const _status = status.toLowerCase();

  if (_status === stopped) return stopped;
  if (_status === pending) return pending;
  if (_status === running) return running;
  if (_status === failed) return failed;

  return stopped;
}

export default createTopicCell;
