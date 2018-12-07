import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import dagreD3 from 'dagre-d3';
import * as d3 from 'd3v4';

import * as _ from 'utils/commonUtils';
import { Box } from 'common/Layout';
import { H5 } from 'common/Headings';
import {
  white,
  blue,
  green,
  lightBlue,
  lightestBlue,
  lighterGray,
  whiteSmoke,
  radiusRounded,
  radiusNormal,
  shadowNormal,
  dimBlue,
} from 'theme/variables';

const Wrapper = styled(Box)`
  width: 65%;
  margin-right: 20px;
  min-height: 800px;
`;

Wrapper.displayName = 'Box';

const H5Wrapper = styled(H5)`
  margin: 0 0 30px;
  font-weight: normal;
  color: ${lightBlue};
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
      border: 1px solid ${whiteSmoke};
    }
  }

  .node-graph {
    cursor: pointer;
  }

  .node-label {
    font-size: 14px;
    color: ${lightBlue};
  }

  .node-topic {
    position: relative;
    width: 60px;
    height: 60px;
    display: flex;
    justify-content: center;
    align-items: center;
    border: 1px solid ${lighterGray};
    border-radius: ${radiusRounded};
    box-shadow: ${shadowNormal};

    .node-text-wrapper {
      position: absolute;
      top: calc(100% + 10px);
    }

    /* type is not necessary for topics */
    .node-type {
      display: none;
    }

    .node-icon {
      color: ${lightestBlue};
    }
  }

  .node-connector {
    width: 200px;
    min-height: 90px;
    padding: 15px 20px;
    border: 1px solid ${lighterGray};
    border-radius: ${radiusNormal};
    box-shadow: ${shadowNormal};
    display: flex;

    .node-icon {
      display: flex;
      justify-content: center;
      align-items: center;
      width: 40px;
      height: 40px;
      margin-right: 8px;
      color: ${white};
      border-radius: ${radiusRounded};
      background-color: ${lightestBlue};
    }

    .node-text-wrapper {
      display: flex;
      flex-direction: column;
      color: ${dimBlue};
    }

    .node-label {
      margin-bottom: 5px;
    }

    .node-type {
      font-size: 11px;
      width: 100px;
      white-space: nowrap;
      text-overflow: ellipsis;
      overflow: hidden;
    }

    &.is-running {
      .node-icon {
        background-color: ${green};
      }
    }
  }

  .fa {
    font-size: 16px;
  }

  .icon-hadoop {
    font-size: 25px;
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
    const { graph } = this.props;

    graph.forEach(({ name, type, to, id, icon, isActive, state = '' }) => {
      const isTopic = type === 'topic';
      const props = { shape: isTopic ? 'circle' : 'rect' };
      const displayType = type.split('.').pop();

      const isActiveCls = isActive ? 'is-active' : '';
      const topicCls = isTopic ? 'node-topic' : 'node-connector';
      const stateCls = !_.isEmptyStr(state) ? `is-${state.toLowerCase()}` : '';

      const html = `<div class="node-graph ${topicCls} ${isActiveCls} ${stateCls}">
        <span class="node-icon"><i class="fa ${icon}"></i></span>
        <div class="node-text-wrapper">
          <span class="node-label">${name}</span>
          <span class="node-type">${displayType}</span>
        </div>
      </div>`;

      g.setNode(id, {
        ...props,
        lable: id,
        labelType: 'html',
        label: html,
        class: isActiveCls,
      });

      if (to) {
        const dests = graph.map(x => x.id);

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
