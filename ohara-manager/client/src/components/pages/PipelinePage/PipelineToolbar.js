import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { ICON_KEYS } from 'constants/pipelines';
import { HadoopIcon } from 'common/Icons';
import { update } from 'utils/pipelineToolbarUtils';
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
  position: relative;
  top: -4px;
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
  margin-right: 20px;
  position: relative;
  top: 5px;

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

FileSavingStatus.displayName = 'FileSavingStatus';

class PipelineToolbar extends React.Component {
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

  update = evtObj => {
    const { updateGraph, graph } = this.props;
    update({ graph, updateGraph, evtObj });
  };

  render() {
    const { hasChanges } = this.props;
    const { jdbcSource, ftpSource, hdfsSink, ftpSink } = ICON_KEYS;
    return (
      <ToolbarWrapper>
        <Sources>
          <Icon
            className="fa fa-upload"
            onClick={this.update}
            data-id={ftpSource}
            data-testid="toolbar-source-ftp"
          />
          <Icon
            className="fa fa-database"
            onClick={this.update}
            data-id={jdbcSource}
            data-testid="toolbar-source"
          />
        </Sources>
        <Sinks>
          <HadoopIconContainer
            onClick={this.update}
            data-id={hdfsSink}
            data-testid="toolbar-sink"
          >
            <HadoopIconWrapper width={28} height={28} fillColor={lightBlue} />
          </HadoopIconContainer>
          <Icon
            className="fa fa-download"
            onClick={this.update}
            data-id={ftpSink}
            data-testid="toolbar-sink-ftp"
          />
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

export default PipelineToolbar;
