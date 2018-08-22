import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { get } from '../../../utils/helpers';
import { HadoopIcon } from '../../common/Icons';
import {
  lightestBlue,
  lighterBlue,
  lightBlue,
  radiusNormal,
  durationNormal,
  blue,
} from '../../../theme/variables';

const Wrapper = styled.div`
  margin-bottom: 15px;
  padding: 15px 20px;
  border: 1px solid ${lightestBlue};
  border-radius: ${radiusNormal};
  display: flex;
  align-items: center;
`;

const IconWrapper = styled.i`
  color: ${lighterBlue};
  font-size: 25px;
  padding: 10px 20px;
  transition: ${durationNormal} all;
  cursor: pointer;

  &:hover,
  &.is-active {
    transition: ${durationNormal} all;
    color: ${blue};
  }
`;

const HadoopIconContainer = styled.i`
  color: ${lighterBlue};
  padding: 10px 20px;
  transition: ${durationNormal} all;
  vertical-align: middle;
  cursor: pointer;

  &:hover svg,
  &.is-active svg {
    transition: ${durationNormal} all;
    fill: ${blue};
  }
`;

const HadoopIconWrapper = styled(HadoopIcon)`
  pointer-events: none;
`;

class Toolbar extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      isExact: PropTypes.bool,
      params: PropTypes.object,
      path: PropTypes.string,
      url: PropTypes.string,
    }).isRequired,
    graph: PropTypes.arrayOf(
      PropTypes.shape({
        type: PropTypes.string,
        uuid: PropTypes.string,
        isActive: PropTypes.bool,
        isExact: PropTypes.bool,
        icon: PropTypes.string,
      }),
    ).isRequired,
    updateGraph: PropTypes.func.isRequired,
  };

  update = e => {
    let type = get(e, 'target.dataset.id', null);
    const isSvg = e.target.nodeName === 'svg';
    const { graph, updateGraph } = this.props;

    let _type = null;
    let update = null;
    type = type ? type : 'sink';

    if (type) {
      _type = graph.find(g => g.type === type);
    }

    if (isSvg) {
      _type = graph.find(g => g.type === 'sink');
    }

    update = { ..._type, isExist: true };
    updateGraph(graph, update, type);
  };

  render() {
    return (
      <Wrapper>
        <IconWrapper
          className="fa fa-database"
          onClick={this.update}
          data-id="source"
        />
        <HadoopIconContainer onClick={this.update} data-id="sink">
          <HadoopIconWrapper width={28} height={28} fillColor={lightBlue} />
        </HadoopIconContainer>
        <IconWrapper
          className="fa fa-list-ul"
          onClick={this.update}
          data-id="topic"
        />
      </Wrapper>
    );
  }
}

export default Toolbar;
