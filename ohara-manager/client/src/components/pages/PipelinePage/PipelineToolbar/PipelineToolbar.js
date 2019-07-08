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
import ReactTooltip from 'react-tooltip';
import { get, includes } from 'lodash';

import * as PIPELINES from 'constants/pipelines';
import PipelineNewStream from './PipelineNewStream';
import PipelineNewConnector from './PipelineNewConnector';
import PipelineNewTopic from './PipelineNewTopic';
import { Modal } from 'components/common/Modal';
import { fetchWorkers } from 'api/workerApi';
import { isEmptyStr } from 'utils/commonUtils';
import { Icon, ToolbarWrapper, FileSavingStatus } from './styles.js';
import { graph as graphPropType } from 'propTypes/pipeline';

const modalNames = {
  ADD_SOURCE_CONNECTOR: 'sources',
  ADD_SINK_CONNECTOR: 'sinks',
  ADD_STREAM: 'streams',
  ADD_TOPIC: 'topics',
};

class PipelineToolbar extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      params: PropTypes.object,
    }).isRequired,
    graph: PropTypes.arrayOf(graphPropType).isRequired,
    updateGraph: PropTypes.func.isRequired,
    hasChanges: PropTypes.bool.isRequired,
    topics: PropTypes.array.isRequired,
    isLoading: PropTypes.bool.isRequired,
    updateCurrentTopic: PropTypes.func.isRequired,
    resetCurrentTopic: PropTypes.func.isRequired,
    currentTopic: PropTypes.object,
    workerClusterName: PropTypes.string.isRequired,
  };

  state = {
    isModalActive: false,
    sources: [],
    sinks: [],
    activeConnector: null,
    connectorType: '',
    isAddBtnDisabled: false,
    isFetchWorkerWorking: true,
    currWorker: null,
  };

  componentDidMount() {
    this.fetchWorker();
    this.modalChild = React.createRef();
  }

  fetchWorker = async () => {
    const { workerClusterName } = this.props;
    const res = await fetchWorkers();
    this.setState({ isFetchWorkerWorking: false });
    const workers = get(res, 'data.result', null);

    if (workers) {
      const currWorker = workers.find(({ name }) => name === workerClusterName);

      if (currWorker) {
        const result = currWorker.connectors.map(connector => {
          const { className, definitions } = connector;
          let targetMeta = {};

          definitions.forEach(meta => {
            const { displayName, defaultValue } = meta;

            if (
              displayName === 'version' ||
              displayName === 'revision' ||
              displayName === 'kind'
            ) {
              targetMeta = {
                ...targetMeta,
                [displayName]: defaultValue,
              };
            }
          });

          const { kind, version, revision } = targetMeta;

          return {
            typeName: kind,
            className,
            version,
            revision,
          };
        });

        const sources = result.filter(
          ({ typeName, className }) =>
            typeName === 'source' &&
            !PIPELINES.CONNECTOR_FILTERS.includes(className),
        );

        const sinks = result.filter(
          ({ typeName, className }) =>
            typeName === 'sink' &&
            !PIPELINES.CONNECTOR_FILTERS.includes(className),
        );

        this.setState({ sources, sinks }, () => {
          // If we have the supported connectors data at hand, let's set the
          // default connector so they can be rendered in connector modal

          const { activeConnector, connectorType = '' } = this.state;
          if (!activeConnector && !isEmptyStr(connectorType)) {
            this.setDefaultConnector(connectorType);
          }
        });
      }
    }
  };

  setDefaultConnector = connectorType => {
    if (connectorType) {
      const { connectorType: connector } = this.state;
      const activeConnector =
        connectorType === 'stream' ? connector : this.state[connector][0];

      this.setState({ activeConnector, isAddBtnDisabled: false });
    }
  };

  handleModalOpen = (modalName, connectorType) => {
    this.setState({ isModalActive: true, modalName, connectorType }, () => {
      this.setDefaultConnector(this.state.connectorType);
    });
  };

  handleModalClose = () => {
    this.setState({ isModalActive: false, activeConnector: null });

    if (this.state.modalName === 'topics') {
      this.props.resetCurrentTopic();
    }
  };

  handleConfirm = () => {
    this.modalChild.current.update();
    if (this.state.modalName === 'topics') {
      this.handleModalClose();
    }
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

  updateAddBtnStatus = currConnector => {
    this.setState({ isAddBtnDisabled: !currConnector });
  };

  render() {
    const {
      hasChanges,
      updateGraph,
      graph,
      topics,
      currentTopic,
      updateCurrentTopic,
      workerClusterName,
    } = this.props;

    const {
      isModalActive,
      modalName,
      connectorType,
      activeConnector,
      isAddBtnDisabled,
      isFetchWorkerWorking,
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
          width={
            modalName === modalNames.ADD_TOPIC ||
            modalName === modalNames.ADD_STREAM
              ? '350px'
              : '600px'
          }
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
              handleClose={this.handleModalClose}
            />
          )}

          {modalName === modalNames.ADD_TOPIC && (
            <PipelineNewTopic
              ref={this.modalChild}
              updateGraph={updateGraph}
              graph={graph}
              topics={topics}
              currentTopic={currentTopic}
              updateTopic={updateCurrentTopic}
              updateAddBtnStatus={this.updateAddBtnStatus}
              workerClusterName={workerClusterName}
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
              updateAddBtnStatus={this.updateAddBtnStatus}
              isLoading={isFetchWorkerWorking}
              workerClusterName={workerClusterName}
              handleClose={this.handleModalClose}
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
