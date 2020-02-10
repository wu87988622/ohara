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
import { createConnection, getCellData } from './PaperUtils';
import { useSnackbar } from 'context';
import { PipelineStateContext } from '../Pipeline';

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

  const showMessage = useSnackbar();
  const { palette } = useTheme();

  const graphRef = React.useRef(null);
  const paperRef = React.useRef(null);

  // Ensure that we only call the event handler once, this prevent paper from
  // making too many network request to the backend service
  const cellAddRef = React.useRef(null);
  const cellChangeRef = React.useRef(null);
  const cellRemoveRef = React.useRef(null);

  // Prevent from getting stale event handlers
  const onCellEventRef = React.useRef(null);

  const [dragStartPosition, setDragStartPosition] = React.useState(null);
  const { isMetricsOn } = React.useContext(PipelineStateContext);

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

  // This paperApi should be passed into all event handlers as the second
  // parameter,  // e.g., onConnect(eventObject, paperApi)
  // the only exception is the onRemove as it doesn't any other parameters.
  const paperApi = ref.current;

  React.useEffect(() => {
    onCellEventRef.current = {
      onElementAdd,
    };
  }, [onElementAdd]);

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
                  sourceElement: getCellData(source),
                  link: getCellData(linkView),
                  targetElement: getCellData(target),
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
      const isSelected = element.$box.find('.menu').is(':visible');
      if (!isSelected) element.unhighlight();
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
            shouldSkipOnChange: true,
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

    // Binding custom event handlers
    // Graph Events
    graph.on('add', cell => {
      if (!_.has(paperApi, 'state.isReady')) return;
      if (_.isEqual(cellAddRef.current, cell)) return;

      // Adding new link is not counted as an `add` event
      if (cell.isLink()) return;

      const data = getCellData(cell);
      cellAddRef.current = cell;

      if (!cell.get('shouldSkipOnElementAdd')) {
        onCellEventRef.current.onElementAdd(data, paperApi);
      }

      // OnChange event handler is called on graph's `add`, `change` and `remove` events
      onChange(paperApi);
    });

    graph.on('change', (cell, updates) => {
      if (!_.has(paperApi, 'state.isReady')) return;
      if (_.isEqual(cellChangeRef.current, updates)) return;
      if (_.has(cell, 'attributes.target.shouldSkipOnChange')) return;

      cellChangeRef.current = updates;
      onChange(paperApi);
    });

    graph.on('remove', cell => {
      if (!_.has(paperApi, 'state.isReady')) return;
      if (_.isEqual(cellRemoveRef.current, cell)) return;
      if (_.has(cell, 'attributes.target.shouldSkipOnChange')) return;

      cellRemoveRef.current = cell;
      onChange(paperApi);
    });

    // Paper events
    paper.on('element:pointerclick', elementView => {
      onCellSelect(getCellData(elementView), paperApi);
      resetElements();
      elementView.showMenu();
      elementView.highlight();

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

        if (result) {
          return onConnect(result, paperApi);
        }

        resetLinks();
      }
    });

    // Element button events, these are custom event binding down in the
    // element like connector or topic
    paper.on('element:link:button:pointerclick', ElementView => {
      paperApi.addLink(ElementView.model.get('id'));
    });

    paper.on('element:start:button:pointerclick', ElementView => {
      onCellStart(getCellData(ElementView), paperApi);
    });

    paper.on('element:stop:button:pointerclick', ElementView => {
      onCellStop(getCellData(ElementView), paperApi);
    });

    paper.on('element:config:button:pointerclick', ElementView => {
      onCellConfig(getCellData(ElementView), paperApi);
    });

    paper.on('element:remove:button:pointerclick', ElementView => {
      onCellRemove(getCellData(ElementView), paperApi);
    });

    paper.on('blank:pointerclick', () => {
      onCellDeselect(paperApi);
      resetElements();
      resetLinks();
    });

    function resetElements() {
      findCellViews().forEach(cellView => {
        cellView.unhighlight();
        cellView.hideMenu();
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
      // graph.off('change');
      // graph.off('remove');

      // Cell events
      paper.off('cell:pointermove');

      // Element events
      paper.off('element:pointerclick');
      paper.off('element:mouseenter');
      paper.off('element:mouseleave');
      paper.off('element:link:button:pointerclick');
      paper.off('element:start:button:pointerclick');
      paper.off('element:stop:button:pointerclick');
      paper.off('element:config:button:pointerclick');
      paper.off('element:remove:button:pointerclick');

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
    onCellConfig,
    onCellDeselect,
    onCellRemove,
    onCellSelect,
    onCellStart,
    onCellStop,
    onChange,
    onConnect,
    onDisconnect,
    palette.grey,
    paperApi,
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
        const { kind } = data;
        const statusColors = {
          stopped: palette.text.secondary,
          pending: palette.warning.main,
          running: palette.success.main,
          failed: palette.error.main,
        };
        const newData = {
          ...data,
          statusColors,
          isMetricsOn,
          paperApi: ref.current,
        };
        let cell;
        if (kind === source || kind === sink || kind === stream) {
          cell = createConnectorCell(newData);
        } else if (kind === topic) {
          cell = createTopicCell(newData);
        } else {
          // invalid type won't be added thru this method (e.g., link)
          throw new Error(
            `paperApi: addElement(data: object) invalid kind from the given data object!`,
          );
        }

        graph.addCell(cell);
        const targetCell = graph.getLastCell();

        return getCellData(targetCell);
      },
      removeElement(id) {
        if (typeof id !== 'string') {
          throw new Error(
            `paperApi: removeElement(id: string) invalid argument id!`,
          );
        }

        const cell = findCell(id);

        if (!cell || !cell.isElement())
          throw new Error(
            `paperApi: removeElement(id: string) can only remove an element with this method`,
          );

        graph.removeCells(cell);
      },
      updateElement(id, data) {
        const targetCell = findCellViews().find(cell => cell.model.id === id);

        if (targetCell) {
          targetCell.updateElement({
            ...targetCell.model.attributes,
            ...data,
          });
        }
      },
      addLink(sourceId, targetId) {
        if (typeof sourceId !== 'string') {
          throw new Error(
            `paperApi: addLink(sourceId: string, targetId: string?) invalid argument sourceId`,
          );
        }

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
          return;
        }

        newLink.target({ id: targetId });
        graph.addCell(newLink);

        return getCellData(newLink);
      },
      removeLink(id) {
        if (typeof id !== 'string') {
          throw new Error(
            `paperApi: removeLink(id: string) invalid argument id`,
          );
        }

        graph.removeCells(graph.getLinks(id));
      },
      getCell(id) {
        if (typeof id !== 'string') {
          throw new Error(`paperApi: getCell(id: string) invalid argument id`);
        }

        const result = findCell(id);

        if (result) return getCellData(result);
      },
      getCells(kind) {
        if (kind) {
          const validTypes = _.values(
            _.pick(KIND, ['source', 'sink', 'stream', 'topic']),
          );

          if (!validTypes.includes(kind))
            throw new Error(
              `paperApi: getCells(kind: string?) argument ${kind} is not a valid type! Available types are: ${validTypes.toString()}`,
            );

          return graph
            .getCells()
            .filter(cell => cell.attributes.kind === kind)
            .map(getCellData);
        }

        return graph.getCells().map(getCellData);
      },
      loadGraph(json) {
        json.cells
          // Elements should be render first, and then the links
          .sort((a, b) => a.type.localeCompare(b.type))
          .forEach(cell => {
            const { type, source, target } = cell;
            if (type === 'html.Element') {
              return this.addElement({ ...cell, shouldSkipOnElementAdd: true });
            }

            if (type === 'standard.Link') {
              this.addLink(source.id, target.id);
            }
          });
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
        const { left, top } = paper.$el.offset();

        return {
          offsetLeft: left,
          offsetTop: top,
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
        if (typeof id !== 'string') {
          throw new Error(`paperApi: center(id: string) invalid argument id!`);
        }

        const element = findCell(id);
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
      toggleMetrics(isOpen) {
        findCellViews()
          .filter(
            ({ model }) =>
              model.get('kind') === KIND.source ||
              model.get('kind') === KIND.sink ||
              model.get('kind') === KIND.stream,
          )
          .forEach(element => element.toggleMetrics(isOpen));
      },

      updateMetrics(objects) {
        objects.map(object =>
          findCellView(object.name).updateMeters(object.metrics),
        );
      },

      highlight(id) {
        if (typeof id !== 'string') {
          throw new Error(
            `paperApi: highlight(id: string) invalid argument id`,
          );
        }

        const cellView = findCellView(id);

        cellView && cellView.highlight();
      },

      enableMenu(id, items) {
        const cellView = findCellView(id);
        cellView && cellView.enableMenu(items);
      },

      disableMenu(id, items) {
        const cellView = findCellView(id);
        cellView && cellView.disableMenu(items);
      },

      // TODO: the state here will be stale, we should update
      // the state will there are updates
      state: {
        isReady: true,
        // Only expose necessary options from JointJS' default
        options: _.pick(paper.options, ['origin']),
      },
    };
  });

  // Private APIs
  function findCellViews() {
    return paperRef.current.findViewsInArea(paperRef.current.getArea());
  }

  function findCellView(nameOrId) {
    return findCellViews().find(
      ({ model }) =>
        model.get('id') === nameOrId || model.get('name') === nameOrId,
    );
  }

  function findCell(nameOrId) {
    return (
      graphRef.current.getCell(nameOrId) ||
      graphRef.current.getCells().find(cell => cell.get('name') === nameOrId)
    );
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
