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

import Toolbar from '../Toolbar';
import Toolbox from '../Toolbox';
import { Paper, PaperWrapper } from './GraphStyles';
import { usePrevious } from 'utils/hooks';

const Graph = props => {
  const { palette } = useTheme();
  const [paperScale, setPaperScale] = useState(1);
  const [isFitToContent, setIsFitToContent] = useState(false);

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

  useEffect(() => {
    const renderGraph = () => {
      graph.current = new joint.dia.Graph();

      // This variable will be used in the future
      // eslint-disable-next-line
      paper.current = new joint.dia.Paper({
        el: document.getElementById('paper'),
        model: graph.current,
        width: '100%',
        height: '100%',
        gridSize: 10,
        drawGrid: { name: 'dot', args: { color: palette.grey[300] } },
        defaultConnectionPoint: { name: 'boundary' },
        background: {
          color: palette.common.white,
        },
        linkPinning: false, // This ensures the link should always link to a valid target
        cellViewNamespace: joint.shapes,
        restrictTranslate: true, // prevent graph from stepping outside of the paper
      });

      paper.current.on('cell:pointerclick', function(elementView) {
        if (!elementView.$box) return;
        resetAll(this);
        elementView.$box.css('boxShadow', `0 0 0 2px ${palette.primary[500]}`);
        elementView.model.attributes.menuDisplay = 'block';
        elementView.updateBox();
        const links = graph.current.getLinks();
        if (links.length > 0) {
          const disConnectLink = links.filter(
            link => !link.attributes.target.id,
          );
          if (disConnectLink.length > 0) {
            disConnectLink[0].target({ id: elementView.model.id });
          }
        }
      });

      paper.current.on('blank:pointerclick', function() {
        resetAll(this);
        resetLink();
      });

      paper.current.on('cell:mouseenter', function(elementView) {
        if (!elementView.$box) return;
        elementView.$box.css('boxShadow', `0 0 0 2px ${palette.primary[500]}`);
      });

      paper.current.on('cell:mouseleave', function(elementView) {
        if (!elementView.$box) return;
        if (elementView.model.attributes.menuDisplay === 'none') {
          elementView.$box.css('boxShadow', '');
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
      });

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

    renderGraph();
  }, [palette.common.white, palette.grey, palette.primary]);

  const setZoom = (scale, instruction = 'fromDropdown') => {
    const fixedScale = Number((Math.floor(scale * 100) / 100).toFixed(2));
    const allowedScales = [0.01, 0.03, 0.06, 0.12, 0.25, 0.5, 1.0, 2.0];
    const isValidScale = allowedScales.includes(fixedScale);

    // Prevent graph from rescaling again
    setIsFitToContent(false);

    if (isValidScale) {
      // If the instruction is `fromDropdown`, we will use the scale it gives
      // and update the state right alway
      if (instruction === 'fromDropdown') return setPaperScale(scale);

      // By default, the scale is multiply and divide by `2`
      const newScale = instruction === 'in' ? fixedScale * 2 : fixedScale / 2;
      return setPaperScale(newScale);
    }

    // Handle `none-valid` scales here
    const defaultScales = [0.5, 1.0, 2.0];
    const closestInScale = defaultScales.reduce((prev, curr) => {
      return Math.abs(curr - fixedScale) < Math.abs(prev - fixedScale)
        ? curr
        : prev;
    });

    // The next value of the `closetInScale` is the out scale
    const inScaleIndex = defaultScales.indexOf(closestInScale);
    const closestOutScale = defaultScales[inScaleIndex + 1];

    const newScale =
      instruction === 'in' ? closestInScale * 2 : closestOutScale / 2;
    setPaperScale(newScale);
  };

  const prevPaperScale = usePrevious(paperScale);
  useEffect(() => {
    // Prevent rescale again
    if (prevPaperScale === paperScale) return;
    if (isFitToContent) return;

    // Update paper scale when `paperScale` updates
    const size = paper.current.getComputedSize();
    paper.current.translate(0, 0);
    paper.current.scale(
      paperScale,
      paperScale,
      size.width / 2,
      size.height / 2,
    );
  }, [isFitToContent, paperScale, prevPaperScale]);

  useEffect(() => {
    if (!isFitToContent) return;

    paper.current.scaleContentToFit({
      padding: 30,
      maxScale: 1,
    });

    // This update is needed so the scale which displays on zoom in/out
    // dropdown will be reflected
    setPaperScale(paper.current.scale().sx);
  }, [isFitToContent]);

  useEffect(() => {
    document.getElementById('paper').addEventListener('mousemove', event => {
      if (isFitToContent) {
        // Reset the state so we can call fit to content multiple times
        setIsFitToContent(false);
      }

      if (
        dragStartPosition.current &&
        dragStartPosition.current.x !== undefined &&
        dragStartPosition.current.y !== undefined
      )
        paper.current.translate(
          event.offsetX - dragStartPosition.current.x,
          event.offsetY - dragStartPosition.current.y,
        );
    });
  }, [isFitToContent]);

  return (
    <>
      <Toolbar
        isToolboxOpen={isToolboxOpen}
        handleToolboxOpen={handleToolboxOpen}
        handleToolbarClick={handleToolbarClick}
        paperScale={paperScale}
        paper={paper.current}
        setZoom={setZoom}
        setFit={() => setIsFitToContent(true)}
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
