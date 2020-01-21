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
import PropTypes from 'prop-types';
import * as joint from 'jointjs';
import _ from 'lodash';
import $ from 'jquery';
import { useTheme } from '@material-ui/core/styles';

import { KIND } from 'const';
import { StyledPaper } from './PaperStyles';
import { createConnectorCell, createTopicCell, createLink } from './cell';
import { createConnection, getReturnedData } from './PaperUtils';
import { useSnackbar } from 'context';

const Paper = React.forwardRef((props, ref) => {
  const {
    onChange = _.noop,
    onCellSelect = _.noop,
    onCellDeselect = _.noop,
    onElementAdd = _.noop,
    onConnect = _.noop,
    onDisconnect = _.noop,

    // Cell events, these are passing down to cells
    onCellStart = _.noop,
    onCellStop = _.noop,
    onCellRemove = _.noop,
    onCellConfig = _.noop,
  } = props;

  const { palette } = useTheme();
  const showMessage = useSnackbar();

  const graphRef = React.useRef(null);
  const paperRef = React.useRef(null);
  const cellRef = React.useRef({});
  // sometime onCellEvent props do not immediate updates
  const onCellEventRef = React.useRef({});
  const [dragStartPosition, setDragStartPosition] = React.useState(null);
  const [hasUnConnectedLink, setHasUnConnectedLink] = React.useState(false);

  React.useEffect(() => {
    const namespace = joint.shapes;
    graphRef.current = new joint.dia.Graph({}, { cellNamespace: namespace });

    paperRef.current = new joint.dia.Paper(
      {
        el: document.getElementById('paper'),
        model: graphRef.current,
        width: '100%',
        height: '100%',

        // Grid settings
        gridSize: 10,
        drawGrid: { name: 'dot', args: { color: palette.grey[300] } },

        background: { color: palette.common.white },

        // Tweak the default highlighting to match our theme
        highlighting: {
          default: {
            name: 'stroke',
            options: {
              padding: 4,
              rx: 4,
              ry: 4,
              attrs: {
                'stroke-width': 2,
                stroke: palette.primary.main,
              },
            },
          },
        },

        // Ensures the link should always link to a valid target
        linkPinning: false,

        // Fixes es6 module issue with JointJS
        cellViewNamespace: joint.shapes,

        // prevent graph from stepping outside of the paper
        restrictTranslate: true,
      },
      { cellNamespace: namespace },
    );
  }, [palette.common.white, palette.grey, palette.primary.main]);

  React.useEffect(() => {
    onCellEventRef.current = {
      onCellStart,
      onCellStop,
      onCellRemove,
      onElementAdd,
    };
  }, [onCellRemove, onCellStart, onCellStop, onElementAdd]);

  React.useEffect(() => {
    //Prevent event from repeating
    const graph = graphRef.current;
    const paper = paperRef.current;

    paper.on('blank:pointerdown', (event, x, y) => {
      setDragStartPosition({
        x: x * paper.scale().sx,
        y: y * paper.scale().sy,
      });
    });

    paper.on('blank:pointermove', event => {
      if (dragStartPosition) {
        paper.translate(
          event.offsetX - dragStartPosition.x,
          event.offsetY - dragStartPosition.y,
        );
      }
    });

    paper.on('blank:pointerup', () => {
      setDragStartPosition(null);
    });

    paper.on('link:mouseenter', linkView => {
      const toolsView = new joint.dia.ToolsView({
        tools: [
          // Allow users to add vertices on link view
          new joint.linkTools.Vertices(),
          new joint.linkTools.Segments(),
          new joint.linkTools.TargetArrowhead(),
          new joint.linkTools.TargetAnchor({
            restrictArea: true,
            areaPadding: 100,
          }),
          new joint.linkTools.Boundary(),
          // Add a custom remove tool
          new joint.linkTools.Remove({
            distance: '50%',
            markup: [
              {
                tagName: 'circle',
                selector: 'button',
                attributes: {
                  r: 8,
                  fill: 'grey',
                  cursor: 'pointer',
                },
              },
              {
                tagName: 'path',
                selector: 'icon',
                attributes: {
                  d: 'M -3 -3 3 3 M -3 3 3 -3',
                  fill: 'none',
                  stroke: '#fff',
                  'stroke-width': 2,
                  'pointer-events': 'none',
                },
              },
            ],
            action(event, linkView, toolView) {
              const source = linkView.model.getSourceElement();
              const target = linkView.model.getTargetElement();
              linkView.model.remove({ ui: true, tool: toolView.cid });
              onDisconnect(
                {
                  sourceElement: getReturnedData(source),
                  targetElement: getReturnedData(target),
                },
                paperApi,
              );
            },
          }),
        ],
      });

      linkView.addTools(toolsView);
      linkView.highlight();
    });

    paper.on('link:mouseleave', linkView => {
      paper.removeTools();
      linkView.unhighlight();
    });

    paper.on('element:mouseenter', element => {
      element.highlight();
    });

    paper.on('element:mouseleave', element => {
      // We might want to keep the state
      if (!element.model.attributes.isMenuDisplayed) {
        element.unhighlight();
      }
    });

    // Create a link that moves along with mouse cursor
    $(document).on('mousemove.createlink', event => {
      if (_.has(paperApi, 'state.isReady')) {
        const link = graph
          .getCells()
          .filter(cell => cell.isLink())
          .find(link => !link.get('target').id);

        if (link) {
          const localPoint = paperApi.getLocalPoint();
          const scale = paperApi.getScale();
          const { offsetLeft, offsetTop } = paperApi.getBbox();

          link.target({
            x: (event.pageX - offsetLeft) / scale.sx + localPoint.x,
            y: (event.pageY - offsetTop) / scale.sy + localPoint.y,
          });

          link.attr({
            // prevent the link from clicking by users, the `root` here is the
            // SVG container element of the link
            root: { style: 'pointer-events: none' },
            line: { stroke: palette.grey[500] },
          });
        }
      }
    });

    // This paperApi should be passed into all event handlers as the second
    // parameter,  // e.g., onConnect(..., paperApi)
    // the only exception is the onRemove as it doesn't any other parameters.
    const paperApi = ref.current;

    // Binding custom handlers
    graph.on('add', cell => {
      if (
        !_.isEqual(cellRef.current, cell) &&
        _.get(paperApi, 'state.isReady', false)
      ) {
        const data = getReturnedData(cell);
        cellRef.current = cell;
        onCellEventRef.current.onElementAdd(data, paperApi);
      }
    });

    graph.on('change', (cell, updates) => {
      onChange({ cell, updates }, paperApi);
    });

    paper.on('element:pointerclick', elementView => {
      onCellSelect(getReturnedData(elementView), paperApi);
      resetCells();
      elementView.highlight();
      elementView.model.attributes.isMenuDisplayed = true;
      elementView.updateBox();

      const sourceLink = graph.getLinks().find(link => !link.get('target').id);
      if (sourceLink) {
        const result = createConnection({
          sourceLink,
          targetElementView: elementView,
          showMessage,
          paperApi,

          // Since createConnection is using quite some JointJS APIs
          // and these APIs are not listed/wrapped in our public APIs
          // so we're passing graph down here
          graph,
        });

        setHasUnConnectedLink(false);

        if (result) {
          return onConnect(result, paperApi);
        }

        resetLinks();
      }
    });

    paper.on('blank:pointerclick', () => {
      onCellDeselect(paperApi);
      resetCells();
      resetLinks();
    });

    function resetCells() {
      getCellViews().forEach(cellView => {
        cellView.unhighlight();
        cellView.model.attributes.isMenuDisplayed = false;
        cellView.updateBox();
      });
    }

    function resetLinks() {
      const links = graph.getLinks();
      if (links.length > 0) {
        // There should only be one
        const unConnectedLink = links.find(link => !link.get('target').id);
        if (unConnectedLink) unConnectedLink.remove();
      }
    }

    return () => {
      // Event should all be cleaned in every render, or it will causes weird
      // behavior in UI

      $(document).off('mousemove.createlink');

      // Graph events
      // graph.off('add');
      graph.off('change');

      // Element events
      paper.off('element:pointerclick');
      paper.off('element:mouseenter');
      paper.off('element:mouseleave');

      // Link
      paper.off('link:pointerclick');
      paper.off('link:mouseenter');
      paper.off('link:mouseleave');

      // Blank events
      paper.off('blank:pointerclick');
      paper.off('blank:pointerdown');
      paper.off('blank:pointerup');
      paper.off('blank:pointermove');
    };
  }, [
    dragStartPosition,
    hasUnConnectedLink,
    onCellDeselect,
    onCellSelect,
    onCellStart,
    onChange,
    onConnect,
    onDisconnect,
    onElementAdd,
    palette.grey,
    ref,
    showMessage,
  ]);

  React.useImperativeHandle(ref, () => {
    const graph = graphRef.current;
    const paper = paperRef.current;

    if (!graph || !paper) return;

    return {
      // Public APIs
      addElement(data) {
        const { source, sink, stream, topic } = KIND;
        const { classType } = data;
        const newData = { ...data, paperApi: ref.current };

        let cell;

        if (
          classType === source ||
          classType === sink ||
          classType === stream
        ) {
          cell = createConnectorCell({
            ...newData,
            onCellStart: onCellEventRef.current.onCellStart,
            onCellStop: onCellEventRef.current.onCellStop,
            onCellConfig,
            onCellRemove: onCellEventRef.current.onCellRemove,
          });
        } else if (classType === topic) {
          cell = createTopicCell({ ...newData, onCellRemove });
        }

        graph.addCell(cell);
        const targetCell = graph.getLastCell();

        return getReturnedData(targetCell);
      },
      removeElement(id) {
        if (typeof id !== 'string')
          throw new Error(
            `paperApi: removeElement(id: string) argument id is not valid!`,
          );

        const cell = graph.getCell(id);
        graph.removeCells(cell);
      },
      updateElement(id, data) {
        const targetCell = getCellViews().find(cell => cell.model.id === id);

        if (targetCell) {
          targetCell.model.attributes = {
            ...targetCell.model.attributes,
            ...data,
          };

          targetCell.updateBox();
        }
      },
      addLink(sourceId, targetId) {
        const newLink = createLink({ sourceId });

        // TargetId is not supplied, meaning, the target is not decided yet,
        // so we will assign mouse position later on. This allows user to control
        // the link
        if (targetId === undefined) {
          // Hide the link when it's first added into Paper. This is because
          // the link position will be set to `0, 0`. We will wait until user
          // moves their cursor and start using the mouse position as link's
          // position
          newLink.attr({
            line: { stroke: 'translate' },
          });

          graph.addCell(newLink);
          setHasUnConnectedLink(true);
          return;
        }

        newLink.target({ id: targetId });
        graph.addCell(newLink);

        return getReturnedData(newLink);
      },
      getCell(id) {
        return getReturnedData(graph.getCell(id));
      },
      getCells(classType) {
        if (classType) {
          const validTypes = _.values(
            _.pick(KIND, ['source', 'sink', 'stream', 'topic']),
          );

          if (!validTypes.includes(classType))
            throw new Error(
              `paperApi: getCells(classType: string?) argument ${classType} is not a valid type! Available types are: ${validTypes.toString()}`,
            );

          return graph
            .getCells()
            .filter(cell => cell.attributes.classType === classType)
            .map(getReturnedData);
        }

        return graph.getCells().map(getReturnedData);
      },
      setScale(sx, sy) {
        if (sx === undefined) {
          throw new Error(
            'paperApi: setScale(sx: number, sy: number) argument sx is undefined!',
          );
        }

        if (sy === undefined) sy = sx;

        paper.scale(sx, sy);
      },
      getScale() {
        return paper.scale(); // getter
      },
      getBbox() {
        const { width, height } = paper.getComputedSize();
        const offset = paper.$el.offset();

        return {
          offsetLeft: offset.left,
          offsetTop: offset.top,
          width,
          height,
        };
      },
      getLocalPoint() {
        return paper.paperToLocalPoint(paper.translate());
      },
      toJson() {
        return graph.toJSON();
      },
      fit() {
        paper.scaleContentToFit({
          padding: 30,
          maxScale: 1,
        });

        return this.getScale().sx;
      },
      center(id) {
        if (typeof id !== 'string')
          throw new Error(
            `paperApi: center(id: string) argument id should be a string!`,
          );

        const element = graph.getCell(id);
        const cellBbox = {
          ...element.getBBox(),
          ...element.getBBox().center(),
        };

        const scale = this.getScale().sx;
        const contentLocalOrigin = paper.paperToLocalPoint(cellBbox);
        const { tx, ty } = paper.translate();
        const { width, height } = this.getBbox();
        const fittingBbox = { x: tx, y: ty, width, height };
        const origin = this.state.options.origin;
        const newOx = fittingBbox.x - contentLocalOrigin.x * scale - origin.x;
        const newOy = fittingBbox.y - contentLocalOrigin.y * scale - origin.y;

        paper.translate(
          // divide by 2 so it's centered
          newOx + fittingBbox.width / 2,
          newOy + fittingBbox.height / 2,
        );
      },

      // TODO: the state here will be stale, we should update
      // the state will there are updates
      state: {
        isReady: true,
        options: paper.options,
      },
    };
  });

  // Private APIs
  function getCellViews() {
    return paperRef.current.findViewsInArea(paperRef.current.getArea());
  }

  return (
    <StyledPaper className={`${dragStartPosition ? 'is-being-grabbed' : ''}`}>
      <div id="paper" />
    </StyledPaper>
  );
});

Paper.propTypes = {
  onChange: PropTypes.func,
  onConnect: PropTypes.func,
  onDisconnect: PropTypes.func,
  onCellSelect: PropTypes.func,
  onCellDeselect: PropTypes.func,
  onElementAdd: PropTypes.func,
  onCellStart: PropTypes.func,
  onCellStop: PropTypes.func,
  onCellRemove: PropTypes.func,
  onCellConfig: PropTypes.func,
};

export default Paper;
