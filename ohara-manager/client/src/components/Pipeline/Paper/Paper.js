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
import { useTheme } from '@material-ui/core/styles';

import { KIND } from 'const';
import { StyledPaper } from './PaperStyles';
import { ConnectorCell, TopicCell } from '../Cells';

const Paper = React.forwardRef((props, ref) => {
  const {
    onCellSelect,
    onCellDeselect,
    onElementAdd,
    onCellStart,
    onCellStop,
    onCellRemove,
    onCellConfig,
  } = props;

  const { palette } = useTheme();

  const graphRef = React.useRef(null);
  const paperRef = React.useRef(null);
  const [dragStartPosition, setDragStartPosition] = React.useState(null);

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

    // Binding handlers
    const paperApi = ref.current;
    graph.on('add', cell => {
      if (_.isFunction(onElementAdd)) onElementAdd(cell, paperApi);
    });

    paper.on('cell:pointerclick', cellView => {
      if (_.isFunction(onCellSelect)) onCellSelect(cellView, paperApi);
      resetCells();

      cellView.model.attributes.isMenuDisplayed = true;
      cellView.updateBox();
    });

    paper.on('blank:pointerclick', () => {
      if (_.isFunction(onCellDeselect)) onCellDeselect(paperApi);
      resetCells();
    });

    function resetCells() {
      getCellViews().forEach(cell => {
        cell.model.attributes.isMenuDisplayed = false;
        cell.updateBox();
      });
    }

    return () => {
      // Graph events
      // graph.off('add');

      // Cell events
      paper.off('cell:pointerclick');

      // Blank events
      paper.off('blank:pointerdown');
      paper.off('blank:pointerclick');
    };
  }, [dragStartPosition, onCellDeselect, onCellSelect, onElementAdd, ref]);

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
          cell = ConnectorCell({
            ...newData,
            onCellStart,
            onCellStop,
            onCellConfig,
            onCellRemove,
          });
        } else if (classType === topic) {
          cell = TopicCell({ ...newData, onCellRemove });
        }

        graph.addCell(cell);
        return graph.getLastCell();
      },
      removeElement(id) {
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
        const link = new joint.shapes.standard.Link();
        link.source({ id: sourceId });
        link.attr({
          line: { stroke: '#9e9e9e' },
        });

        if (targetId === undefined) {
          return graph.addCell(link);
        }

        link.target({ id: targetId });
        return graph.addCell(link);
      },
      getCells(classType) {
        if (classType) {
          return graph.getCells().filter(cell => {
            return cell.attributes.classType === classType;
          });
        }

        return graph.getCells();
      },
      scale(newScale) {
        if (newScale === undefined) return paper.scale(); // getter
        paper.scale(newScale); // setter
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

        return this.scale().sx;
      },
      center(currentCell) {
        const cellBbox = {
          ...currentCell.getBBox(),
          ...currentCell.getBBox().center(),
        };

        const scale = this.scale().sx;
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

  return <StyledPaper id="paper" />;
});

Paper.propTypes = {
  onCellSelect: PropTypes.func,
  onCellDeselect: PropTypes.func,
  onElementAdd: PropTypes.func,
  onCellStart: PropTypes.func,
  onCellStop: PropTypes.func,
  onCellRemove: PropTypes.func,
  onCellConfig: PropTypes.func,
};

export default Paper;
