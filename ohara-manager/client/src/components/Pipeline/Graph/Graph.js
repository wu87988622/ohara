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

import React, { useEffect, useRef } from 'react';
import { useTheme } from '@material-ui/core/styles';
import { isNumber } from 'lodash';
import PropTypes from 'prop-types';
import * as joint from 'jointjs';

import Toolbar from '../Toolbar';
import Toolbox from '../Toolbox';
import { Paper, PaperWrapper } from './GraphStyles';

const Graph = props => {
  const { palette } = useTheme();
  const [paperScale, setPaperScale] = React.useState(1);
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
  let setZoom = useRef(null);
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
        drawGrid: { name: 'dot', args: { color: palette.grey[400] } },
        defaultConnectionPoint: { name: 'boundary' },
        background: {
          color: palette.common.white,
        },
        linkPinning: false,
        cellViewNamespace: joint.shapes,
        restrictTranslate: true,
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
        dragStartPosition.current = {
          x: x * paperScale,
          y: y * paperScale,
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

      setZoom.current = scale => {
        if (isNumber(scale)) {
          return setPaperScale(scale);
        }

        if (scale === 'fit') {
          paper.current.scaleContentToFit({ padding: 30 });

          // scale() returns {sx, sy}, both value are the same in our App
          setPaperScale(paper.current.scale().sx);
        }
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
    // eslint-disable-next-line
  }, [palette.common.white, palette.grey, palette.primary]);

  useEffect(() => {
    const size = paper.current.getComputedSize();
    paper.current.translate(0, 0);
    paper.current.scale(
      paperScale,
      paperScale,
      size.width / 2,
      size.height / 2,
    );
  }, [paperScale]);

  useEffect(() => {
    document.getElementById('paper').addEventListener('mousemove', event => {
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
  }, []);

  return (
    <>
      <Toolbar
        isToolboxOpen={isToolboxOpen}
        handleToolboxOpen={handleToolboxOpen}
        handleToolbarClick={handleToolbarClick}
        paperScale={paperScale}
        setZoom={setZoom.current}
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
