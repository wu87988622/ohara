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
import styled from 'styled-components';
import PropTypes from 'prop-types';
import dagreD3 from 'dagre-d3';
import * as d3 from 'd3v4';

import * as _ from 'utils/commonUtils';
import { Box } from 'common/Layout';
import { H5 } from 'common/Headings';
import { getIcon, getStatusIcon } from './pipelineUtils/pipelineGraphUtils';

const Wrapper = styled(Box)`
  width: 65%;
  margin-right: 20px;
  min-height: 800px;
`;

Wrapper.displayName = 'Box';

const H5Wrapper = styled(H5)`
  margin: 0 0 30px;
  font-weight: normal;
  color: ${props => props.theme.lightBlue};
`;

H5Wrapper.displayName = 'H5Wrapper';

const Svg = styled.svg`
  width: 100%;
  height: 100%;

  .node {
    circle,
    rect {
      fill: transparent;
      cursor: pointer;
      border: 1px solid ${props => props.theme.whiteSmoke};
    }
    foreignObject {
      /* Make topic name visible */
      overflow: visible;
    }
  }

  .node-graph {
    cursor: pointer;
  }

  .node-name {
    font-size: 14px;
    color: ${props => props.theme.lightBlue};
  }

  .node-topic {
    position: relative;
    width: 60px;
    height: 60px;
    display: flex;
    justify-content: center;
    align-items: center;
    border: 1px solid ${props => props.theme.lighterGray};
    border-radius: ${props => props.theme.radiusRounded};
    box-shadow: ${props => props.theme.shadowNormal};

    .node-text-wrapper {
      position: absolute;
      top: calc(100% + 10px);
    }

    /* These labels are not needed in topics */
    .node-type,
    .node-status {
      display: none;
    }

    .node-icon {
      color: ${props => props.theme.dimBlue};
    }
    .status-icon {
      display: none;
    }
  }

  .node-connector {
    width: 200px;
    min-height: 90px;
    padding: 15px 20px;
    border: 1px solid ${props => props.theme.lighterGray};
    border-radius: ${props => props.theme.radiusNormal};
    box-shadow: ${props => props.theme.shadowNormal};
    display: flex;

    .node-icon {
      display: flex;
      justify-content: center;
      align-items: center;
      width: 40px;
      height: 40px;
      margin-right: 8px;
      color: ${props => props.theme.white};
      border-radius: ${props => props.theme.radiusRounded};
      background-color: ${props => props.theme.lightestBlue};
    }

    .node-text-wrapper {
      display: flex;
      flex-direction: column;
      color: ${props => props.theme.dimBlue};
    }

    .node-name {
      margin-bottom: 5px;
      width: 110px;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .node-status {
      font-size: 11px;
      color: ${props => props.theme.lighterBlue};
      margin-bottom: 5px;
    }

    .node-type {
      font-size: 11px;
      width: 100px;
      white-space: nowrap;
      text-overflow: ellipsis;
      overflow: hidden;
    }

    .status-icon {
      position: absolute;
      width: 16px;
      height: 16px;
      right: 8px;
      top: 7px;
      display: none;
    }

    &.is-running {
      .node-icon {
        background-color: ${props => props.theme.green};
      }
    }

    &.is-failed {
      .node-icon {
        background-color: ${props => props.theme.red};
      }
      .status-icon {
        color: ${props => props.theme.red};
        display: block;
      }
    }
  }

  .fa {
    font-size: 16px;
  }

  path {
    stroke: ${props => props.theme.lighterGray};
    fill: ${props => props.theme.lighterGray};
    stroke-width: 2px;
  }
`;

Svg.displayName = 'Svg';

class PipelineGraph extends React.Component {
  static propTypes = {
    graph: PropTypes.arrayOf(
      PropTypes.shape({
        type: PropTypes.string,
        name: PropTypes.string,
        id: PropTypes.string,
        isActive: PropTypes.bool,
        icon: PropTypes.string,
      }),
    ).isRequired,
    pipeline: PropTypes.shape({
      workerClusterName: PropTypes.string,
    }).isRequired,
    resetGraph: PropTypes.func.isRequired,
    updateGraph: PropTypes.func.isRequired,
    match: PropTypes.shape({
      isExact: PropTypes.bool,
      params: PropTypes.object,
      path: PropTypes.string,
      url: PropTypes.string,
    }).isRequired,
    history: PropTypes.object,
  };

  componentDidMount() {
    this.renderGraph();
  }

  componentDidUpdate(prevProps) {
    if (this.props.graph !== prevProps.graph) {
      this.renderGraph();
    }
  }

  handleNodeClick = currId => {
    const { history, graph, match } = this.props;
    const { pipelineId } = match.params;

    const [currConnector] = graph.filter(g => g.id === currId);

    const { kind, id: connectorId } = currConnector;

    const action = match.url.includes('/edit/') ? 'edit' : 'new';
    const baseUrl = `/pipelines/${action}/${kind}/${pipelineId}`;

    if (connectorId) {
      history.push(`${baseUrl}/${connectorId}`);
    } else {
      history.push(`${baseUrl}`);
    }
  };

  renderGraph = () => {
    const { graph, pipeline = {} } = this.props;
    const { workerClusterName } = pipeline;
    const g = new dagreD3.graphlib.Graph().setGraph({});

    graph.forEach(({ name, kind, to, id, isActive = false, state = '' }) => {
      const isTopic = kind === 'topic';
      const props = { shape: isTopic ? 'circle' : 'rect' };
      const displayKind = kind.split('.').pop();

      const isActiveCls = isActive ? 'is-active' : '';
      const topicCls = isTopic ? 'node-topic' : 'node-connector';
      const stateCls = !_.isEmptyStr(state) ? `is-${state.toLowerCase()}` : '';
      const status = !_.isEmptyStr(state) ? state.toLowerCase() : 'stopped';
      const icon = getIcon(kind);
      const statusIcon = getStatusIcon(state);

      const html = `<div class="node-graph ${topicCls} ${isActiveCls} ${stateCls}">
        <span class="node-icon"><i class="fa ${icon}"></i></span>
        <div class="node-text-wrapper">
          <span class="node-name">${name}</span>
          <span class="node-status">Status: ${status}</span>
          <span class="node-type">${displayKind}</span>
        </div>
        <a class="status-icon" href="/logs/workers/${workerClusterName}" target="_blank">
          <i class="fas ${statusIcon}"></i>
        </a>
      </div>`;

      g.setNode(id, {
        ...props,
        lable: id,
        labelType: 'html',
        label: html,
        class: isActiveCls,
      });

      if (to) {
        // Get dest graphs
        const dests = graph.map(x => x.id);

        // If the dest graphs are not listed in the graph object
        // or it's not an array, return at this point
        if (!dests.includes(to) && !Array.isArray(to)) return;

        if (Array.isArray(to)) {
          // Exclude '?' as they're not valid targets
          to.filter(t => t !== '?').forEach(t => {
            g.setEdge(id, t, {});
          });
          return;
        }

        g.setEdge(id, to, {});
      }
    });

    const svg = d3.select('.pipeline-graph');
    const inner = svg.select('g');

    const zoom = d3.zoom().on('zoom', () => {
      inner.attr('transform', d3.event.transform);
    });

    svg.call(zoom);

    const render = new dagreD3.render();

    g.setGraph({
      rankdir: 'LR',
      marginx: 50,
      marginy: 50,
    });

    render(inner, g);

    svg.selectAll('.node').on('click', this.handleNodeClick);
  };

  render() {
    return (
      <Wrapper>
        <H5Wrapper>Pipeline graph</H5Wrapper>
        <Svg className="pipeline-graph">
          <g />
        </Svg>
      </Wrapper>
    );
  }
}
export default PipelineGraph;
