import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import * as d3 from 'd3v4';
import dagreD3 from 'dagre-d3';

import { Box } from 'common/Layout';
import { H5 } from 'common/Headings';
import { lightBlue, lightestBlue, whiteSmoke, blue } from 'theme/variables';

const H5Wrapper = styled(H5)`
  margin: 0 0 30px;
  font-weight: normal;
  color: ${lightBlue};
`;

H5Wrapper.displayName = 'H5Wrapper';

const Svg = styled.svg`
  width: 100%;
  height: 260px;

  .node circle {
    fill: ${whiteSmoke};
    cursor: pointer;
  }

  .node.is-active circle {
    fill: ${blue};
  }

  .fa {
    cursor: pointer;
    color: ${lightestBlue};
    font-size: 16px;
  }

  .icon-hadoop {
    font-size: 25px;
  }

  text {
    fill: white;
    text-transform: uppercase;
  }

  path {
    stroke: ${blue};
    fill: ${blue};
    stroke-width: 2px;
  }
`;

class PipelineGraph extends React.Component {
  static propTypes = {
    graph: PropTypes.arrayOf(
      PropTypes.shape({
        type: PropTypes.string,
        name: PropTypes.string,
        uuid: PropTypes.string,
        isActive: PropTypes.bool,
        icon: PropTypes.string,
      }),
    ).isRequired,
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

  handleNodeClick = id => {
    const { history, resetGraph, updateGraph, graph, match } = this.props;
    const { topicId, pipelineId, sourceId, sinkId } = match.params;

    resetGraph();
    updateGraph({ isActive: true }, id);

    const [_graph] = graph.filter(g => g.id === id);

    const action = match.url.includes('/edit/') ? 'edit' : 'new';
    const baseUrl = `/pipelines/${action}/${
      _graph.type
    }/${pipelineId}/${topicId}`;

    if (sinkId) {
      history.push(`${baseUrl}/${sourceId}/${sinkId}`);
    } else if (sourceId) {
      history.push(`${baseUrl}/${sourceId}`);
    } else {
      history.push(`${baseUrl}`);
    }
  };

  renderGraph = () => {
    const g = new dagreD3.graphlib.Graph().setGraph({});

    this.props.graph.forEach(({ to, id, icon, isActive }) => {
      const props = { width: 60, height: 60, shape: 'circle' };

      const isActiveCls = isActive ? 'is-active' : '';
      const html = `<div class="node-graph ${isActiveCls}">
        <i class="fa ${icon}"></i>
      </div>`;

      g.setNode(id, {
        ...props,
        lable: id,
        labelType: 'html',
        label: html,
        class: isActiveCls,
      });

      if (to) {
        const dests = this.props.graph.map(x => x.id);

        if (!dests.includes(to)) return;

        if (Array.isArray(to)) {
          to.forEach(t => {
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
      <Box>
        <H5Wrapper>Pipeline graph</H5Wrapper>
        <Svg className="pipeline-graph">
          <g />
        </Svg>
      </Box>
    );
  }
}
export default PipelineGraph;
