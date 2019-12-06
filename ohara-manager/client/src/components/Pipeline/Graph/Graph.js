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

import React, { useEffect, useRef, useState } from 'react';
import { useTheme } from '@material-ui/core/styles';
import PropTypes from 'prop-types';
import * as joint from 'jointjs';

import { usePrevious } from 'utils/hooks';
import Toolbar from '../Toolbar';
import Toolbox from '../Toolbox';
import { Paper, PaperWrapper } from './GraphStyles';
import { updateCurrentCell } from './graphUtils';
import { useZoom, useCenter } from './GraphHooks';

const Graph = props => {
  const { palette } = useTheme();
  const [hasSelectedCell, setHasSelectedCell] = useState(false);
  const {
    setZoom,
    paperScale,
    setPaperScale,
    isFitToContent,
    setIsFitToContent,
  } = useZoom();

  const { setCenter, isCentered, setIsCentered } = useCenter();

  const {
    isToolboxOpen,
    toolboxExpanded,
    handleToolboxClick,
    handleToolbarClick,
    handleToolboxOpen,
    handleToolboxClose,
    toolboxKey,
    setToolboxExpanded,
  } = props;

  let graph = useRef(null);
  let paper = useRef(null);
  let dragStartPosition = useRef(null);
  let currentCell = useRef(null);

  useEffect(() => {
    const renderGraph = () => {
      graph.current = new joint.dia.Graph();
      paper.current = new joint.dia.Paper({
        el: document.getElementById('paper'),
        model: graph.current,
        width: '100%',
        height: '100%',
        gridSize: 10,
        origin: { x: 0, y: 0 },
        drawGrid: { name: 'dot', args: { color: palette.grey[300] } },
        defaultConnectionPoint: { name: 'boundary' },
        background: {
          color: palette.common.white,
        },
        linkPinning: false, // This ensures the link should always link to a valid target
        cellViewNamespace: joint.shapes,
        restrictTranslate: true, // prevent graph from stepping outside of the paper
      });

      paper.current.on('cell:pointerclick', cellView => {
        currentCell.current = {
          cellView,
          bBox: {
            ...cellView.getBBox(),
            ...cellView.getBBox().center(),
          },
        };

        setHasSelectedCell(true);

        if (!cellView.$box) return;

        resetAll(paper.current);
        cellView.$box.css('boxShadow', `0 0 0 2px ${palette.primary[500]}`);
        cellView.model.attributes.menuDisplay = 'block';
        cellView.updateBox();
        const links = graph.current.getLinks();

        if (links.length > 0) {
          const disConnectLink = links.filter(
            link => !link.attributes.target.id,
          );
          if (disConnectLink.length > 0) {
            disConnectLink[0].target({ id: cellView.model.id });
          }
        }
      });

      paper.current.on('blank:pointerclick', () => {
        resetAll(paper.current);
        resetLink();
        currentCell.current = null;
        setHasSelectedCell(false);
      });

      paper.current.on('cell:mouseenter', cellView => {
        if (!cellView.$box) return;
        cellView.$box.css('boxShadow', `0 0 0 2px ${palette.primary[500]}`);
      });

      paper.current.on('cell:mouseleave', cellView => {
        if (!cellView.$box) return;
        if (cellView.model.attributes.menuDisplay === 'none') {
          cellView.$box.css('boxShadow', '');
        }
      });

      paper.current.on('blank:pointerdown', (event, x, y) => {
        // Using the scales from paper itself instead of our
        // paperScale state since it will cause re-render
        // which destroy all graphs on current paper...
        dragStartPosition.current = {
          x: x * paper.current.scale().sx,
          y: y * paper.current.scale().sy,
        };
      });

      paper.current.on('cell:pointerup blank:pointerup', () => {
        if (dragStartPosition.current) {
          delete dragStartPosition.current.x;
          delete dragStartPosition.current.y;
        }

        updateCurrentCell(currentCell);
        setIsCentered(false);
      });
    };

    renderGraph();
  }, [palette.common.white, palette.grey, palette.primary, setIsCentered]);

  const resetAll = paper => {
    const views = paper._views;
    paper.model.getElements().forEach(element => {
      element.attributes.menuDisplay = 'none';
    });
    Object.keys(paper._views).forEach(key => {
      if (!views[key].$box) return;
      views[key].updateBox();
      views[key].$box.css('boxShadow', '');
    });
  };

  const resetLink = () => {
    const links = graph.current.getLinks();
    if (links.length > 0) {
      const disConnectLink = links.filter(link => !link.attributes.target.id);
      if (disConnectLink.length > 0) {
        disConnectLink[0].remove();
      }
    }
  };

  const prevPaperScale = usePrevious(paperScale);
  useEffect(() => {
    // Prevent rescale again
    if (prevPaperScale === paperScale) return;
    if (isFitToContent) return;

    paper.current.scale(paperScale);

    updateCurrentCell(currentCell);
    setIsCentered(false);
  }, [isFitToContent, paperScale, prevPaperScale, setIsCentered]);

  useEffect(() => {
    if (!isFitToContent) return;

    paper.current.scaleContentToFit({
      padding: 30,
      maxScale: 1,
    });

    // This update is needed so the scale which displays on zoom in/out
    // dropdown will be reflected
    setPaperScale(paper.current.scale().sx);
    updateCurrentCell(currentCell);
    setIsCentered(false);
  }, [isFitToContent, setIsCentered, setPaperScale]);

  useEffect(() => {
    document.getElementById('paper').addEventListener('mousemove', event => {
      // Reset the state so we can call fit to content multiple times
      if (isFitToContent) setIsFitToContent(false);

      if (
        dragStartPosition.current &&
        dragStartPosition.current.x &&
        dragStartPosition.current.y
      ) {
        paper.current.translate(
          event.offsetX - dragStartPosition.current.x,
          event.offsetY - dragStartPosition.current.y,
        );
      }
    });
  }, [isFitToContent, setIsFitToContent]);

  return (
    <>
      <Toolbar
        isToolboxOpen={isToolboxOpen}
        handleToolboxOpen={handleToolboxOpen}
        handleToolbarClick={handleToolbarClick}
        paperScale={paperScale}
        handleZoom={setZoom}
        handleFit={() => setIsFitToContent(true)}
        handleCenter={() => {
          // We don't want to re-center again
          if (!isCentered) {
            setCenter({ paper, currentCell, paperScale });

            setIsFitToContent(false);
            setIsCentered(true);
          }
        }}
        hasSelectedCell={hasSelectedCell}
      />
      <PaperWrapper>
        <Paper id="paper"></Paper>
        <Toolbox
          isOpen={isToolboxOpen}
          expanded={toolboxExpanded}
          handleClick={handleToolboxClick}
          handleClose={handleToolboxClose}
          paper={paper.current}
          graph={graph.current}
          toolboxKey={toolboxKey}
          setToolboxExpanded={setToolboxExpanded}
        />
      </PaperWrapper>
    </>
  );
};

Graph.propTypes = {
  isToolboxOpen: PropTypes.bool.isRequired,
  toolboxExpanded: PropTypes.shape({
    topic: PropTypes.bool.isRequired,
    source: PropTypes.bool.isRequired,
    sink: PropTypes.bool.isRequired,
    stream: PropTypes.bool.isRequired,
  }).isRequired,
  handleToolboxClick: PropTypes.func.isRequired,
  handleToolboxOpen: PropTypes.func.isRequired,
  handleToolbarClick: PropTypes.func.isRequired,
  handleToolboxClose: PropTypes.func.isRequired,
  toolboxKey: PropTypes.number.isRequired,
  setToolboxExpanded: PropTypes.func.isRequired,
};

export default Graph;
