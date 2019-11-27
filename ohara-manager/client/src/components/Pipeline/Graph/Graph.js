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
import PropTypes from 'prop-types';
import * as joint from 'jointjs';
import MuiTheme from 'theme/muiTheme';

import Toolbox from '../Toolbox';
import { Paper } from './GraphStyles';

const Graph = props => {
  const {
    isToolboxOpen,
    toolboxExpanded,
    handleToolboxClick,
    handleToolboxClose,
    toolboxKey,
    setToolboxExpanded,
  } = props;

  let graph = useRef(null);
  let paper = useRef(null);

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
        drawGrid: { name: 'dot', args: { color: 'black' } },
        defaultConnectionPoint: { name: 'boundary' },
        background: {
          color: 'rgb(245, 245, 245, .1)',
        },
        linkPinning: false,
        cellViewNamespace: joint.shapes,
        restrictTranslate: true,
      });

      paper.current.on('cell:pointerclick', function(elementView) {
        if (!elementView.$box) return;
        resetAll(this);
        elementView.$box.css(
          'boxShadow',
          `0 0 0 2px ${MuiTheme.palette.primary[500]}`,
        );
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
        elementView.$box.css(
          'boxShadow',
          `0 0 0 2px ${MuiTheme.palette.primary[500]}`,
        );
      });

      paper.current.on('cell:mouseleave', function(elementView) {
        if (!elementView.$box) return;
        if (elementView.model.attributes.menuDisplay === 'none') {
          elementView.$box.css('boxShadow', '');
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
  }, [toolboxKey]);

  return (
    <>
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
      <Paper id="paper"></Paper>
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
  handleToolboxClose: PropTypes.func.isRequired,
  toolboxKey: PropTypes.number.isRequired,
  setToolboxExpanded: PropTypes.func.isRequired,
};

export default Graph;
