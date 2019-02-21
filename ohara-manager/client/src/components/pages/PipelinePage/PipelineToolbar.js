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

import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import ReactTooltip from 'react-tooltip';
import { get, includes } from 'lodash';

import * as PIPELINES from 'constants/pipelines';
import PipelineNewStream from './PipelineNewStream';
import PipelineNewConnector from './PipelineNewConnector';
import PipelineNewTopic from './PipelineNewTopic';
import { Modal } from 'common/Modal';
import { fetchCluster } from 'api/clusterApi';

const ToolbarWrapper = styled.div`
  margin-bottom: 15px;
  padding: 15px 30px;
  border: 1px solid ${props => props.theme.lightestBlue};
  border-radius: ${props => props.theme.radiusNormal};
  display: flex;
  align-items: center;
`;

ToolbarWrapper.displayName = 'ToolbarWrapper';

const Icon = styled.i`
  color: ${props => props.theme.lighterBlue};
  font-size: 25px;
  margin-right: 20px;
  transition: ${props => props.theme.durationNormal} all;
  cursor: pointer;

  &:hover,
  &.is-active {
    transition: ${props => props.theme.durationNormal} all;
    color: ${props => props.theme.blue};
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
  color: ${props => props.theme.lighterBlue};
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
    topics: PropTypes.array.isRequired,
    isLoading: PropTypes.bool.isRequired,
    updateCurrentTopic: PropTypes.func.isRequired,
    currentTopic: PropTypes.object,
  };

  state = {
    isModalActive: false,
    sources: [],
    sinks: [],
    activeConnector: null,
    connectorType: '',
    isAddBtnDisabled: false,
  };

  componentDidMount() {
    this.fetchCluster();
    this.modalChild = React.createRef();
  }

  fetchCluster = async () => {
    const res = await fetchCluster();

    const result = get(res, 'data.result', null);

    if (!result) return;

    const sources = result.sources.filter(
      source => !PIPELINES.CONNECTOR_FILTERS.includes(source.className),
    );

    const sinks = result.sinks.filter(
      sink => !PIPELINES.CONNECTOR_FILTERS.includes(sink.className),
    );

    this.setState({ sources, sinks });
  };

  setDefaultConnector = connectorType => {
    if (connectorType) {
      const { connectorType: connector } = this.state;
      const activeConnector =
        connectorType === 'stream' ? connector : this.state[connector][0];

      this.setState({ activeConnector });
    }
  };

  handleModalOpen = (modalName, connectorType) => {
    this.setState({ isModalActive: true, modalName, connectorType }, () => {
      this.setDefaultConnector(this.state.connectorType);
    });
  };

  handleModalClose = () => {
    this.setState({ isModalActive: false, activeConnector: null });
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

  updateAddBtnStatus = streamAppId => {
    this.setState({ isAddBtnDisabled: !streamAppId });
  };

  render() {
    const {
      hasChanges,
      updateGraph,
      graph,
      topics,
      currentTopic,
      updateCurrentTopic,
      isLoading,
    } = this.props;

    const {
      isModalActive,
      modalName,
      connectorType,
      activeConnector,
      isAddBtnDisabled,
    } = this.state;

    const { ftpSource } = PIPELINES.CONNECTOR_TYPES;

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
          width={modalName === modalNames.ADD_TOPIC ? '350px' : '600px'}
          handleCancel={this.handleModalClose}
          handleConfirm={this.handleConfirm}
          confirmBtnText="Add"
          showActions={true}
          isConfirmDisabled={isAddBtnDisabled}
        >
          {modalName === modalNames.ADD_STREAM && (
            <PipelineNewStream
              {...this.props}
              activeConnector={activeConnector}
              updateAddBtnStatus={this.updateAddBtnStatus}
              ref={this.modalChild}
            />
          )}

          {modalName === modalNames.ADD_TOPIC && (
            <PipelineNewTopic
              updateGraph={updateGraph}
              graph={graph}
              topics={topics}
              currentTopic={currentTopic}
              isLoading={isLoading}
              updateTopic={updateCurrentTopic}
              ref={this.modalChild}
            />
          )}

          {includes(
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
          data-tip="Add a source connector"
          onClick={() =>
            this.handleModalOpen(modalNames.ADD_SOURCE_CONNECTOR, 'sources')
          }
          data-id={ftpSource}
          data-testid="toolbar-sources"
        />
        <Icon
          className="fas fa-list-ul"
          data-tip="Add a topic"
          onClick={() => this.handleModalOpen(modalNames.ADD_TOPIC)}
          data-id={modalNames.ADD_TOPIC}
          data-testid="toolbar-topics"
        />
        <Icon
          className="fas fa-wind"
          data-tip="Add a stream app"
          onClick={() => this.handleModalOpen(modalNames.ADD_STREAM, 'stream')}
          data-id={modalNames.ADD_STREAM}
          data-testid="toolbar-streams"
        />
        <Icon
          className="fas fa-file-export"
          data-tip="Add a sink connector"
          onClick={() =>
            this.handleModalOpen(modalNames.ADD_SINK_CONNECTOR, 'sinks')
          }
          data-id={ftpSource}
          data-testid="toolbar-sinks"
        />

        <ReactTooltip />

        <FileSavingStatus>
          {hasChanges ? 'Saving...' : 'All changes saved'}
        </FileSavingStatus>
      </ToolbarWrapper>
    );
  }
}

export default PipelineToolbar;
