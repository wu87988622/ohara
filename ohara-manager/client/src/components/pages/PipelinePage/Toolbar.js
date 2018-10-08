import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { v4 as uuid4 } from 'uuid';

import * as _ from 'utils/helpers';
import { HadoopIcon } from 'common/Icons';
import {
  lightestBlue,
  lighterBlue,
  lightBlue,
  radiusNormal,
  durationNormal,
  blue,
} from 'theme/variables';

const ToolbarWrapper = styled.div`
  margin-bottom: 15px;
  padding: 10px;
  border: 1px solid ${lightestBlue};
  border-radius: ${radiusNormal};
  display: flex;
  align-items: center;
`;

ToolbarWrapper.displayName = 'ToolbarWrapper';

const Sources = styled.div`
  padding: 10px 20px;
  border-right: 1px solid ${lightestBlue};
`;

const Topics = styled.div`
  padding: 10px 20px;
  border-right: 1px solid ${lightestBlue};
`;

const Sinks = styled.div`
  padding: 10px 20px;
  border-right: 1px solid ${lightestBlue};
`;

const Icon = styled.i`
  color: ${lighterBlue};
  font-size: 25px;
  margin-right: 20px;
  transition: ${durationNormal} all;
  cursor: pointer;

  &:hover,
  &.is-active {
    transition: ${durationNormal} all;
    color: ${blue};
  }

  &:last-child {
    border-right: none;
    margin-right: 0;
  }
`;

Icon.displayName = 'Icon';

const HadoopIconContainer = styled.i`
  color: ${lighterBlue};
  transition: ${durationNormal} all;
  vertical-align: middle;
  cursor: pointer;

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
    iconMaps: PropTypes.shape({
      source: PropTypes.string,
      topic: PropTypes.string,
      sink: PropTypes.string,
    }).isRequired,
  };

  update = e => {
    const { updateGraph, iconMaps } = this.props;

    let type = _.get(e, 'target.dataset.id', null);
    type = type ? type : 'sink';

    const update = {
      name: `Untitled ${type}`,
      type,
      to: '?',
      isActive: false,
      icon: iconMaps[type],
      id: uuid4(),
    };

    // TODO: remove this when pipeline new graph is ready!
    if (type === 'source-ftp') return;

    updateGraph(update, type);
  };

  render() {
    const { hasChanges } = this.props;
    return (
      <ToolbarWrapper>
        <Sources>
          <Icon
            className="fa fa-upload"
            onClick={this.update}
            data-id="source-ftp"
            data-testid="toolbar-source-ftp"
          />
          <Icon
            className="fa fa-database"
            onClick={this.update}
            data-id="source"
            data-testid="toolbar-source"
          />
        </Sources>
        <Sinks>
          <HadoopIconContainer
            onClick={this.update}
            data-id="sink"
            data-testid="toolbar-sink"
          >
            <HadoopIconWrapper width={28} height={28} fillColor={lightBlue} />
          </HadoopIconContainer>
        </Sinks>
        <Topics>
          <Icon
            className="fa fa-list-ul"
            onClick={this.update}
            data-id="topic"
            data-testid="toolbar-topic"
          />
        </Topics>
        <FileSavingStatus>
          {hasChanges ? 'Saving...' : 'All changes saved'}
        </FileSavingStatus>
      </ToolbarWrapper>
    );
  }
}

export default Toolbar;
