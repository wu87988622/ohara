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
    onCellRemove,
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
      statusColor={statusColors[status]}
      width={56}
      height={56}
    />,
  );
  const sharedIcon = renderToString(
    <SharedTopicIcon
      statusColor={statusColors[status]}
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
        <div class="topic-menu">
          <Button class="topic-link">${linkIcon}</Button>
          <Button class="topic-remove">${removeIcon}</Button> 
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

      const $linkButton = $box.find('.topic-link');
      const $removeButton = $box.find('.topic-remove');

      const id = this.model.id;

      // Menu actions
      $linkButton.on('click', () => paperApi.addLink(id));
      $removeButton.on('click', () => {
        const elementData = paperApi.getCell(id);
        onCellRemove(elementData, paperApi);
      });

      this.updatePosition();
      return this;
    },
    openMenu() {
      this.$box.find('.topic-menu').show();
    },
    closeMenu() {
      this.$box.find('.topic-menu').hide();
    },
    updateElement(cellData) {
      const { status } = cellData;
      const $box = this.$box;
      $box.find('.display-name').text(displayName);
      $box.find('.topic-status').attr('fill', statusColors[status]);
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

export default createTopicCell;
