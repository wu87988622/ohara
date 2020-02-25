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
import _ from 'lodash';
import $ from 'jquery';
import * as joint from 'jointjs';
import { useTheme } from '@material-ui/core/styles';

import { KIND } from 'const';
import { StyledPaper } from './PaperStyles';
import { createConnectorCell, createTopicCell, createLink } from './cell';
import { useSnackbar } from 'context';
import { PipelineStateContext } from '../Pipeline';
import * as paperUtils from './PaperUtils';

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

  const hoverHighlighter = {
    highlighter: {
      name: 'stroke',
      options: {
        padding: 2,
        rx: 2,
        ry: 2,
        attrs: {
          'stroke-width': 2,
          stroke: palette.action.active,
        },
      },
    },
  };

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

    paper.on('blank:mouseover', () => {
      findElementViews().forEach(hideMenu);
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
                  sourceElement: paperUtils.getCellData(source),
                  link: paperUtils.getCellData(linkView),
                  targetElement: paperUtils.getCellData(target),
                },
                paperApi,
              );
            },
          }),
        ],
      });

      linkView.addTools(toolsView);
      linkView.highlight(null, hoverHighlighter);
    });

    paper.on('link:mouseleave', linkView => {
      paper.removeTools();
      linkView.unhighlight(null, hoverHighlighter);
    });

    paper.on('element:mouseenter', elementView => {
      showMenu(elementView, hoverHighlighter);
    });

    paper.on('element:mouseleave', (elementView, event) => {
      const width = elementView.$box.width();
      const height = elementView.$box.height();
      const { left, top } = elementView.$box.offset();
      const { clientX, clientY } = event;

      // mouseenter and mouseleave events both get triggered again when hovering
      // on menu's buttons. This creates a flickering effect which looks a lot like a
      // UI defect. Here, we're calculating if the mouse is still moving inside the
      // paper element, if so. We don't fire the event again. This does fix the issue
      // but if user's mouse moves too fast, the bug could be appeared.
      const isOutSideOfTheElement =
        clientX > width + left ||
        clientY > height + top ||
        clientX < left ||
        clientY < top;

      if (isOutSideOfTheElement) hideMenu(elementView);
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

      const data = paperUtils.getCellData(cell);
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
      resetElements();

      elementView
        .highlight()
        .showElement('menu')
        .hideElement('metrics')
        .hideElement('status')
        .setIsSelected(true);

      onCellSelect(paperUtils.getCellData(elementView), paperApi);

      const sourceLink = graph.getLinks().find(link => !link.get('target').id);
      if (sourceLink) {
        const result = paperUtils.createConnection({
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
    paper.on('element:link:button:pointerclick', elementView => {
      paperApi.addLink(elementView.model.get('id'));
    });

    paper.on('element:start:button:pointerclick', elementView => {
      onCellStart(paperUtils.getCellData(elementView), paperApi);
    });

    paper.on('element:stop:button:pointerclick', elementView => {
      onCellStop(paperUtils.getCellData(elementView), paperApi);
    });

    paper.on('element:config:button:pointerclick', elementView => {
      onCellConfig(paperUtils.getCellData(elementView), paperApi);
      hideMenu(elementView);
    });

    paper.on('element:remove:button:pointerclick', elementView => {
      onCellRemove(paperUtils.getCellData(elementView), paperApi);
    });

    paper.on('blank:pointerclick', () => {
      resetElements();
      resetLinks();
      onCellDeselect(paperApi);
    });

    function showMenu(elementView, highlighter) {
      elementView.showElement('menu').highlight(null, highlighter);

      if (elementView.model.get('isMetricsOn')) {
        return elementView.hideElement('metrics');
      }
      elementView.hideElement('status');
    }

    function hideMenu(elementView) {
      elementView.unhighlight(null, hoverHighlighter).hideElement('menu');

      if (elementView.model.get('isMetricsOn')) {
        return elementView.showElement('metrics');
      }
      elementView.showElement('status');
    }

    function resetElements() {
      findElementViews().forEach(elementView => {
        elementView
          .unhighlight()
          .unhighlight(null, hoverHighlighter)
          .hideElement('menu')
          .setIsSelected(false);

        if (elementView.model.get('isMetricsOn')) {
          return elementView.showElement('metrics');
        }
        elementView.showElement('status');
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

      // TODO: the event should be unsubscribed here, but doing so will cause
      // paper losing all elements from the screen

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
      paper.off('blank:mouseover');
    };
  }, [
    dragStartPosition,
    hoverHighlighter,
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

        return paperUtils.getCellData(targetCell);
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
        const targetCell = findElementViews().find(
          cell => cell.model.id === id,
        );

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

        return paperUtils.getCellData(newLink);
      },
      removeLink(id) {
        if (typeof id !== 'string') {
          throw new Error(
            `paperApi: removeLink(id: string) invalid argument id`,
          );
        }

        graph.removeCells(graph.getCell(id));
      },
      getCell(id) {
        if (typeof id !== 'string') {
          throw new Error(`paperApi: getCell(id: string) invalid argument id`);
        }

        const result = findCell(id);

        if (result) return paperUtils.getCellData(result);
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
            .map(paperUtils.getCellData);
        }

        return graph.getCells().map(paperUtils.getCellData);
      },
      loadGraph(json) {
        json.cells
          // Elements should be render first, and then the links
          .sort((a, b) => a.type.localeCompare(b.type))
          .forEach(cell => {
            const { type, source, target } = cell;
            if (type === 'html.Element') {
              return this.addElement({
                ...cell,
                shouldSkipOnElementAdd: true,
                isSelected: false, // we don't want to recovery this state for now
              });
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
        const elementView = findElementView(id);
        const elementBbox = {
          ...elementView.getBBox(),
          ...elementView.getBBox().center(),
        };

        const scale = this.getScale().sx;
        const contentLocalOrigin = paper.paperToLocalPoint(elementBbox);
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
        findElementViews()
          .filter(
            ({ model }) =>
              model.get('kind') === KIND.source ||
              model.get('kind') === KIND.sink ||
              model.get('kind') === KIND.stream,
          )
          .forEach(element => element.toggleMetrics(isOpen));
      },

      updateMetrics(elementArray) {
        elementArray.map(element =>
          findElementView(element.name).updateMeters(element.metrics),
        );
      },

      highlight(id) {
        if (typeof id !== 'string') {
          throw new Error(
            `paperApi: highlight(id: string) invalid argument id`,
          );
        }

        const elementView = findElementView(id);
        if (elementView) {
          findElementViews().forEach(elementView =>
            elementView
              .unhighlight()
              .unhighlight(null, hoverHighlighter)
              .setIsSelected(false),
          );

          elementView.highlight().setIsSelected(true);
        }
      },

      enableMenu(id, items) {
        const elementView = findElementView(id);
        elementView && elementView.enableMenu(items);
      },

      disableMenu(id, items) {
        const elementView = findElementView(id);
        elementView && elementView.disableMenu(items);
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
  function findElementViews() {
    const result = graphRef.current
      .getElements()
      .map(element => paperRef.current.findViewByModel(element.get('id')));

    return result;
  }

  function findElementView(nameOrId) {
    return findElementViews().find(
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
