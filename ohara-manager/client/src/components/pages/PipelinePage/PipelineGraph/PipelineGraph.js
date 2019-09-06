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

import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import dagreD3 from 'dagre-d3';
import * as d3 from 'd3v4';

import * as utils from './pipelineGraphUtils';
import { Wrapper, H5Wrapper, Svg } from './styles';
import { graph as graphPropType } from 'propTypes/pipeline';

const PipelineGraph = props => {
  useEffect(() => {
    const renderGraph = () => {
      const { graph, pipeline = {} } = props;
      const {
        tags: { workerClusterName },
      } = pipeline;
      const dagreGraph = new dagreD3.graphlib.Graph().setGraph({});

      graph.forEach(g => {
        const { name, className, kind, to, state, metrics, isActive } = g;

        // Topic needs different props...
        const nodeProps = { shape: kind === 'topic' ? 'circle' : 'rect' };

        const html = utils.createHtml({
          name,
          className,
          kind,
          state,
          metrics,
          isActive,
          workerClusterName,
        });

        dagreGraph.setNode(name, {
          ...nodeProps,
          labelType: 'html',
          label: html,
        });

        if (to) {
          // Get dest graphs
          const destinations = graph.map(g => g.name);

          // If the destinations graphs are not listed in the graph object
          // or it's not an array, return at this point
          if (!destinations.includes(to) && !Array.isArray(to)) return;

          if (Array.isArray(to)) {
            to.forEach(t => {
              // The pipeline API now requires the `to` key to be an array of object
              // but the connector topic is still an array containing string...
              const topicName = typeof t === 'object' ? t.name : t;

              dagreGraph.setEdge(name, topicName, {});
            });
            return;
          }

          dagreGraph.setEdge(name, to, {});
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

      svg.selectAll('.node').on('click', handleNodeClick);
    };

    const handleNodeClick = current => {
      const { history, graph, match, updateGraph } = props;
      const { workspaceName, pipelineName } = match.params;
      const currentConnector = graph.find(g => g.name === current);
      const { name: connectorName, kind } = currentConnector;

      updateGraph({
        update: currentConnector,
        dispatcher: { name: 'GRAPH' },
      });

      const baseUrl = `/pipelines/edit/${workspaceName}/${pipelineName}`;

      if (connectorName) {
        history.push(`${baseUrl}/${kind}/${connectorName}`);
      } else {
        history.push(`${baseUrl}`);
      }
    };

    renderGraph();
  }, [props, props.graph]);

  return (
    <Wrapper data-testid="pipeline-graph">
      <H5Wrapper>Pipeline graph</H5Wrapper>
      <Svg className="pipeline-graph">
        <g />
      </Svg>
    </Wrapper>
  );
};

PipelineGraph.propTypes = {
  graph: PropTypes.arrayOf(graphPropType).isRequired,
  pipeline: PropTypes.shape({
    workerClusterName: PropTypes.string,
  }).isRequired,
  updateGraph: PropTypes.func.isRequired,
  match: PropTypes.shape({
    params: PropTypes.object.isRequired,
  }).isRequired,
  history: PropTypes.object,
};

export default PipelineGraph;
