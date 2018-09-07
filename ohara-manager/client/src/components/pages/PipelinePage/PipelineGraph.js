import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import cx from 'classnames';

import { Box } from '../../common/Layout';
import { H5 } from '../../common/Headings';
import { HadoopIcon } from '../../common/Icons';
import * as _ from '../../../utils/helpers';

import {
  white,
  radiusRounded,
  lightBlue,
  lightestBlue,
  whiteSmoke,
  blue,
  blueHover,
  durationNormal,
} from '../../../theme/variables';

const H5Wrapper = styled(H5)`
  margin: 0 0 30px;
  font-weight: normal;
  color: ${lightBlue};
`;

H5Wrapper.displayName = 'H5Wrapper';

const Graph = styled.ul`
  display: flex;
  position: relative;
  justify-content: space-between;
  align-items: center;
`;

Graph.displayName = 'Graph';

const Node = styled.li`
  width: 60px;
  height: 60px;
  border-radius: ${radiusRounded};
  background-color: ${whiteSmoke};
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  z-index: 50;
  visibility: ${props => (props.isExist ? 'visible' : 'hidden')};

  &:hover {
    background-color: ${blue};
    transition: ${durationNormal} all;

    .fas {
      color: ${white};
    }
  }

  &.is-exist {
    visibility: visible;
  }

  &.is-active {
    background-color: ${blue};
    transition: ${durationNormal} all;

    .fas {
      color: ${white};
    }

    &:hover {
      transition: ${durationNormal} all;
      background-color: ${blueHover};
    }
  }

  &:last-child {
    &:after {
      display: none;
    }
  }
`;

Node.displayName = 'Node';

const Separator = styled.div`
  height: 3px;
  flex: 1;
  background: ${blue};
  margin: 0 5px;
  visibility: ${props => (props.isActive ? 'visible' : 'hidden')};
`;

const IconWrapper = styled.i`
  position: relative;
  z-index: 50;
  color: ${lightestBlue};
`;

IconWrapper.displayName = 'IconWrapper';

class PipelineGraph extends React.Component {
  static propTypes = {
    graph: PropTypes.arrayOf(
      PropTypes.shape({
        type: PropTypes.string,
        uuid: PropTypes.string,
        isActive: PropTypes.bool,
        isExist: PropTypes.bool,
        icon: PropTypes.string,
      }),
    ).isRequired,
    resetGraph: PropTypes.func.isRequired,
    updateGraph: PropTypes.func.isRequired,
  };

  componentDidMount() {
    const { match, updateGraph } = this.props;
    const page = _.get(match, 'params.page', null);
    const sourceId = _.get(match, 'params.sourceId', null);
    const topicId = _.get(match, 'params.topicId', null);
    const sinkId = _.get(match, 'params.sinkId', null);

    if (page) {
      updateGraph({ isActive: true, isExist: true }, page);
    }

    if (topicId) {
      updateGraph({ isExist: true }, 'topic');
    }

    if (sourceId && sourceId !== '__') {
      updateGraph({ isExist: true }, 'source');
      updateGraph({ isActive: true }, 'separator-1');
    }

    if (sinkId) {
      updateGraph({ isExist: true }, 'sink');
      updateGraph({ isActive: true }, 'separator-2');
    }
  }

  handleClick = e => {
    const { nodeName } = e.target;
    const { history, resetGraph, graph, match } = this.props;
    const { topicId, pipelineId, sourceId, sinkId } = match.params;

    const path =
      nodeName !== 'LI'
        ? 'target.parentElement.dataset.id'
        : 'target.dataset.id';
    const page = _.get(e, path, null);

    const activePage = graph.find(g => g.isActive === true);
    const isUpdate = activePage.type !== page;

    if (page && isUpdate) {
      resetGraph(graph);
      const baseUrl = `/pipeline/new/${page}/${pipelineId}`;

      if (sinkId) {
        history.push(`${baseUrl}/${topicId}/${sourceId}/${sinkId}`);
      } else if (sourceId) {
        history.push(`${baseUrl}/${topicId}/${sourceId}`);
      } else {
        history.push(`${baseUrl}/${topicId}`);
      }
    }
  };

  renderGraph = ({ type, isExist, isActive, icon }, idx) => {
    const nodeCls = cx({ 'is-exist': isExist, 'is-active': isActive });
    const separatorCls = cx({ 'is-exist': true });
    const iconCls = `fas ${icon}`;

    if (type.indexOf('separator') > -1) {
      return (
        <Separator className={separatorCls} isActive={isActive} key={idx} />
      );
    } else {
      return (
        <Node
          key={idx}
          className={nodeCls}
          onClick={this.handleClick}
          data-id={type}
          data-testid={`graph-${type}`}
        >
          {type === 'sink' ? (
            <HadoopIcon width={28} height={28} fillColor={lightestBlue} />
          ) : (
            <IconWrapper className={iconCls} />
          )}
        </Node>
      );
    }
  };

  render() {
    return (
      <Box>
        <H5Wrapper>Pipeline graph</H5Wrapper>
        <Graph data-testid="graph-list">
          {this.props.graph.map((g, idx) => this.renderGraph(g, idx))}
        </Graph>
      </Box>
    );
  }
}
export default PipelineGraph;
