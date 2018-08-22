import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import cx from 'classnames';

import { Box } from '../../common/Layout';
import { H5 } from '../../common/Heading';
import { HadoopIcon } from '../../common/Icons';
import { get, isNull } from '../../../utils/helpers';

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
  justify-content: center;
  position: relative;

  &.is-multiple {
    justify-content: space-between;
  }
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
  z-index: 100;
  display: none;

  &:hover {
    background-color: ${blue};
    transition: ${durationNormal} all;

    .fas {
      color: ${white};
    }
  }

  &.is-exist {
    display: flex;
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

const IconWrapper = styled.i`
  position: relative;
  z-index: 100;
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
  };

  state = {
    isMultiple: false,
  };

  componentDidUpdate(next) {
    if (this.props.graph !== next.graph) {
      const { graph } = this.props;
      const actives = graph.filter(g => !!g.isExist);
      if (actives.length > 1) this.setState({ isMultiple: true });
    }
  }

  handleClick = e => {
    const { nodeName } = e.target;
    const { history, resetGraph, graph } = this.props;
    const path =
      nodeName !== 'LI'
        ? 'target.parentElement.dataset.id'
        : 'target.dataset.id';
    const page = get(e, path, null);

    const activePage = graph.find(g => g.isActive === true);
    const isUpdate = activePage.type !== page;

    if (!isNull(page) && isUpdate) {
      resetGraph(graph);
      history.push(`/pipeline/new/${page}`);
    }
  };

  render() {
    return (
      <Box>
        <H5Wrapper>Pipeline graph</H5Wrapper>

        <Graph className={this.state.isMultiple ? 'is-multiple' : ''}>
          {this.props.graph.map(({ type, isExist, isActive, icon }) => {
            const nodeCls = cx({ 'is-exist': isExist, 'is-active': isActive });
            const iconCls = isExist ? `fas ${icon}` : '';

            return (
              <Node
                key={type}
                className={nodeCls}
                onClick={this.handleClick}
                data-id={type}
              >
                {type === 'sink' && isExist ? (
                  <HadoopIcon width={28} height={28} fillColor={lightestBlue} />
                ) : (
                  <IconWrapper className={iconCls} />
                )}
              </Node>
            );
          })}
        </Graph>
      </Box>
    );
  }
}
export default PipelineGraph;
