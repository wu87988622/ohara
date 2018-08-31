import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { HadoopIcon } from '../../common/Icons';
import {
  lightestBlue,
  lighterBlue,
  lightBlue,
  radiusNormal,
  durationNormal,
  blue,
} from '../../../theme/variables';
import * as _ from '../../../utils/helpers';

const ToolbarWrapper = styled.div`
  margin-bottom: 15px;
  padding: 10px;
  border: 1px solid ${lightestBlue};
  border-radius: ${radiusNormal};
  display: flex;
  align-items: center;
`;

ToolbarWrapper.displayName = 'ToolbarWrapper';

const IconWrapper = styled.i`
  color: ${lighterBlue};
  font-size: 25px;
  padding: 10px 20px;
  transition: ${durationNormal} all;
  cursor: pointer;
  border-right: 1px solid ${lightestBlue};

  &:hover,
  &.is-active {
    transition: ${durationNormal} all;
    color: ${blue};
  }

  &:last-child {
    border-right: none;
  }
`;

IconWrapper.displayName = 'IconWrapper';

const HadoopIconContainer = styled.i`
  color: ${lighterBlue};
  padding: 10px 20px;
  transition: ${durationNormal} all;
  vertical-align: middle;
  cursor: pointer;
  border-right: 1px solid ${lightestBlue};

  &:hover svg,
  &.is-active svg {
    transition: ${durationNormal} all;
    fill: ${blue};
  }
`;

HadoopIconContainer.displayName = 'HadoopIconContainer';

const HadoopIconWrapper = styled(HadoopIcon)`
  pointer-events: none;
`;

HadoopIconWrapper.displayName = 'HadoopIconWrapper';

const FileSavingStatus = styled.div`
  margin-left: 30px;
  color: red;
  font-size: 12px;
  color: ${lighterBlue};
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
    hasChanges: PropTypes.bool.isRequired,
  };

  update = e => {
    let type = _.get(e, 'target.dataset.id', null);
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
    const { hasChanges } = this.props;
    return (
      <ToolbarWrapper>
        <IconWrapper
          className="fa fa-database"
          onClick={this.update}
          data-id="source"
          data-testid="toolbar-source"
        />
        <HadoopIconContainer
          onClick={this.update}
          data-id="sink"
          data-testid="toolbar-sink"
        >
          <HadoopIconWrapper width={28} height={28} fillColor={lightBlue} />
        </HadoopIconContainer>
        <IconWrapper
          className="fa fa-list-ul"
          onClick={this.update}
          data-id="topic"
          data-testid="toolbar-topic"
        />

        <FileSavingStatus>
          {hasChanges ? 'Saving...' : 'All changes saved'}
        </FileSavingStatus>
      </ToolbarWrapper>
    );
  }
}

export default Toolbar;
