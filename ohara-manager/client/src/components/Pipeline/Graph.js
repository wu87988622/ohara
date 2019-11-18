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
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import * as joint from 'jointjs';

import Toolbox from './Toolbox';

const Paper = styled.div(
  ({ theme }) => css`
    border: ${theme.spacing(1)}px solid #fff;

    .flying-paper {
      border: 1px dashed ${theme.palette.grey[400]};
      box-shadow: ${theme.shadows[8]};
      display: flex;
      align-items: center;
      opacity: 0.85;
      z-index: ${theme.zIndex.tooltip};
      padding: ${theme.spacing(0, 2)};

      .item {
        height: auto !important;
        display: flex;
        align-items: center;
        width: 100%;

        .icon {
          margin-right: ${theme.spacing(1)}px;
        }

        label {
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }
    }
  `,
);

const Graph = props => {
  const {
    isToolboxOpen,
    toolboxExpanded,
    handleToolboxClick,
    handleToolboxClose,
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
    };

    renderGraph();
  }, []);

  return (
    <>
      <Toolbox
        isOpen={isToolboxOpen}
        expanded={toolboxExpanded}
        handleClick={handleToolboxClick}
        handleClose={handleToolboxClose}
        paper={paper.current}
        graph={graph.current}
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
    streamApp: PropTypes.bool.isRequired,
  }).isRequired,
  handleToolboxClick: PropTypes.func.isRequired,
  handleToolboxClose: PropTypes.func.isRequired,
};

export default Graph;
