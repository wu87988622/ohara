import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import * as _ from 'utils/commonUtils';
import * as CSS_VARS from 'theme/variables';
import * as PIPELINES from 'constants/pipelines';
import { Modal } from 'common/Modal';
import { fetchCluster } from 'utils/pipelineToolbarUtils';
import PipelineNewStream from './PipelineNewStream';
import PipelineNewConnector from './PipelineNewConnector';
import PipelineNewTopic from './PipelineNewTopic';

const ToolbarWrapper = styled.div`
  margin-bottom: 15px;
  padding: 15px 30px;
  border: 1px solid ${CSS_VARS.lightestBlue};
  border-radius: ${CSS_VARS.radiusNormal};
  display: flex;
  align-items: center;
`;

ToolbarWrapper.displayName = 'ToolbarWrapper';

const Icon = styled.i`
  color: ${CSS_VARS.lighterBlue};
  font-size: 25px;
  margin-right: 20px;
  transition: ${CSS_VARS.durationNormal} all;
  cursor: pointer;

  &:hover,
  &.is-active {
    transition: ${CSS_VARS.durationNormal} all;
    color: ${CSS_VARS.blue};
  }

  &:last-child {
    border-right: none;
    margin-right: 0;
  }
`;

Icon.displayName = 'Icon';

const FileSavingStatus = styled.div`
  margin-left: auto;
  color: red;
  font-size: 12px;
  color: ${CSS_VARS.lighterBlue};
`;

FileSavingStatus.displayName = 'FileSavingStatus';

const modalNames = {
  ADD_SOURCE_CONNECTOR: 'sources',
  ADD_SINK_CONNECTOR: 'sinks',
  ADD_STREAM: 'streams',
  ADD_TOPIC: 'topics',
};

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
        id: PropTypes.string,
        isActive: PropTypes.bool,
        isExact: PropTypes.bool,
        icon: PropTypes.string,
      }),
    ).isRequired,
    updateGraph: PropTypes.func.isRequired,
    hasChanges: PropTypes.bool.isRequired,
  };

  state = {
    isModalActive: false,
    sources: [],
    sinks: [],
    activeConnector: {},
    connectorType: '',
  };

  componentDidMount() {
    this.fetchCluster();
    this.modalChild = React.createRef();
  }

  fetchCluster = async () => {
    const res = await fetchCluster();

    const sources = res.sources.filter(
      source => !PIPELINES.CONNECTOR_FILTERS.includes(source.className),
    );

    const sinks = res.sinks.filter(
      sink => !PIPELINES.CONNECTOR_FILTERS.includes(sink.className),
    );

    this.setState({ sources, sinks });
  };

  setDefaultConnector = connectorType => {
    if (connectorType) {
      this.setState({ activeConnector: this.state[connectorType][0] });
    }
  };

  handleModalOpen = (modalName, connectorType) => {
    this.setState({ isModalActive: true, modalName, connectorType }, () => {
      this.setDefaultConnector(this.state.connectorType);
    });
  };

  handleModalClose = () => {
    this.setState({ isModalActive: false });
  };

  handleConfirm = () => {
    this.modalChild.current.update();
    this.handleModalClose();
  };

  handleTrSelect = name => {
    this.setState(prevState => {
      const { connectorType } = prevState;
      const active = prevState[connectorType].filter(
        connector => connector.className === name,
      );
      return {
        activeConnector: active[0],
      };
    });
  };

  render() {
    const { hasChanges, updateGraph, graph } = this.props;
    const { ftpSource } = PIPELINES.CONNECTOR_KEYS;
    const {
      isModalActive,
      modalName,
      connectorType,
      activeConnector,
    } = this.state;

    const getModalTitle = () => {
      switch (modalName) {
        case modalNames.ADD_STREAM:
          return 'Add a new stream app';
        case modalNames.ADD_TOPIC:
          return 'Add a new topic';
        default: {
          const _connectorType = connectorType.substring(
            0,
            connectorType.length - 1,
          );
          return `Add a new ${_connectorType} connector`;
        }
      }
    };

    return (
      <ToolbarWrapper>
        <Modal
          title={getModalTitle()}
          isActive={isModalActive}
          width="600px"
          handleCancel={this.handleModalClose}
          handleConfirm={this.handleConfirm}
          confirmBtnText="Add"
          showActions={true}
        >
          {modalName === modalNames.ADD_STREAM && (
            <PipelineNewStream {...this.props} ref={this.modalChild} />
          )}

          {modalName === modalNames.ADD_TOPIC && (
            <PipelineNewTopic
              updateGraph={updateGraph}
              graph={graph}
              ref={this.modalChild}
            />
          )}

          {_.includes(
            [modalNames.ADD_SOURCE_CONNECTOR, modalNames.ADD_SINK_CONNECTOR],
            modalName,
          ) && (
            <PipelineNewConnector
              ref={this.modalChild}
              connectorType={connectorType}
              connectors={this.state[connectorType]}
              activeConnector={activeConnector}
              onSelect={this.handleTrSelect}
              updateGraph={updateGraph}
              graph={graph}
            />
          )}
        </Modal>

        <Icon
          className="fas fa-file-import"
          onClick={() =>
            this.handleModalOpen(modalNames.ADD_SOURCE_CONNECTOR, 'sources')
          }
          data-id={ftpSource}
          data-testid="toolbar-sources"
        />
        <Icon
          className="fas fa-list-ul"
          onClick={() => this.handleModalOpen(modalNames.ADD_TOPIC)}
          data-id={modalNames.ADD_TOPIC}
          data-testid="toolbar-topics"
        />
        <Icon
          className="fas fa-wind"
          onClick={() => this.handleModalOpen(modalNames.ADD_STREAM)}
          data-id={modalNames.ADD_STREAM}
          data-testid="toolbar-streams"
        />
        <Icon
          className="fas fa-file-export"
          onClick={() =>
            this.handleModalOpen(modalNames.ADD_SINK_CONNECTOR, 'sinks')
          }
          data-id={ftpSource}
          data-testid="toolbar-sinks"
        />

        <FileSavingStatus>
          {hasChanges ? 'Saving...' : 'All changes saved'}
        </FileSavingStatus>
      </ToolbarWrapper>
    );
  }
}

export default PipelineToolbar;
