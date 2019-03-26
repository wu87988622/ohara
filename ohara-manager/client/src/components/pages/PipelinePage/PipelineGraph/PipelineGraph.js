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
import dagreD3 from 'dagre-d3';
import * as d3 from 'd3v4';

import * as _ from 'utils/commonUtils';
import { Wrapper, H5Wrapper, Svg } from './styles';
import { getIcon, getStatusIcon } from '../pipelineUtils/pipelineGraphUtils';

class PipelineGraph extends React.Component {
  static propTypes = {
    graph: PropTypes.arrayOf(
      PropTypes.shape({
        className: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired,
        id: PropTypes.string.isRequired,
        kind: PropTypes.string.isRequired,
        to: PropTypes.arrayOf(PropTypes.string).isRequired,
      }),
    ).isRequired,
    pipeline: PropTypes.shape({
      workerClusterName: PropTypes.string,
    }).isRequired,
    resetGraph: PropTypes.func.isRequired,
    updateGraph: PropTypes.func.isRequired,
    match: PropTypes.shape({
      params: PropTypes.object.isRequired,
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
    const { className, id: connectorId } = currConnector;

    const action = match.url.includes('/edit/') ? 'edit' : 'new';
    const baseUrl = `/pipelines/${action}/${className}/${pipelineId}`;

    if (connectorId) {
      history.push(`${baseUrl}/${connectorId}`);
    } else {
      history.push(`${baseUrl}`);
    }
  };

  renderGraph = () => {
    const { graph, pipeline = {} } = this.props;
    const { workerClusterName } = pipeline;
    const dagreGraph = new dagreD3.graphlib.Graph().setGraph({});

    graph.forEach(g => {
      const { name, className, kind, to, id, state } = g;

      const updateState = state ? state : '';
      const isTopic = kind === 'topic';

      // Topic needs different props...
      const props = { shape: isTopic ? 'circle' : 'rect' };
      const displayKind = className.split('.').pop();

      const topicCls = isTopic ? 'node-topic' : 'node-connector';
      const stateCls = !_.isEmptyStr(updateState)
        ? `is-${updateState.toLowerCase()}`
        : '';
      const status = !_.isEmptyStr(updateState)
        ? updateState.toLowerCase()
        : 'stopped';
      const icon = getIcon(kind);
      const statusIcon = getStatusIcon(updateState);

      const html = `<div class="node-graph ${topicCls} ${stateCls}">
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

      dagreGraph.setNode(id, {
        ...props,
        lable: id,
        labelType: 'html',
        label: html,
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
            dagreGraph.setEdge(id, t, {});
          });
          return;
        }

        dagreGraph.setEdge(id, to, {});
      }
    });

    const svg = d3.select('.pipeline-graph');
    const inner = svg.select('g');

    const zoom = d3.zoom().on('zoom', () => {
      inner.attr('transform', d3.event.transform);
    });

    svg.call(zoom);

    const render = new dagreD3.render();

    dagreGraph.setGraph({
      rankdir: 'LR',
      marginx: 50,
      marginy: 50,
    });

    render(inner, dagreGraph);

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
