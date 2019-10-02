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

import React, { useState, useEffect, createRef } from 'react';
import PropTypes from 'prop-types';
import Tooltip from '@material-ui/core/Tooltip';

import * as PIPELINES from 'constants/pipelines';
import PipelineNewStream from './PipelineNewStream';
import PipelineNewConnector from './PipelineNewConnector';
import PipelineNewTopic from './PipelineNewTopic';
import { Dialog } from 'components/common/Mui/Dialog';
import { Icon, ToolbarWrapper, FileSavingStatus } from './styles.js';
import { graph as graphPropType } from 'propTypes/pipeline';

const PipelineToolbar = props => {
  const [isModalActive, setIsModalActive] = useState(false);
  const [sources, setSources] = useState([]);
  const [sinks, setSinks] = useState([]);
  const [activeConnector, setActiveConnector] = useState(null);
  const [connectorType, setConnectorType] = useState('');
  const [isAddBtnDisabled, setIsAddBtnDisabled] = useState(true);
  const [modalName, setModalName] = useState('');

  const modalChild = createRef(null);

  const {
    connectors,
    hasChanges,
    workerClusterName,
    match,
    updateGraph,
    topics,
    graph,
  } = props;

  useEffect(() => {
    const getConnectorInfo = async () => {
      const result = connectors.map(connector => {
        const { className, definitions } = connector;
        let targetDefinition = {};

        definitions.forEach(definition => {
          const { displayName, defaultValue } = definition;

          if (
            displayName === 'version' ||
            displayName === 'revision' ||
            displayName === 'kind'
          ) {
            targetDefinition = {
              ...targetDefinition,
              [displayName]: defaultValue,
            };
          }
        });

        const { kind, version, revision } = targetDefinition;

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

      setSources(sources);
      setSinks(sinks);
    };

    getConnectorInfo();
  }, [activeConnector, connectorType, connectors]);

  const handleModalOpen = (modalName, connectorType) => {
    setIsModalActive(true);
    setConnectorType(connectorType);
    setModalName(modalName);
  };

  useEffect(() => {
    const targetTypes = ['source', 'sink'];
    if (
      isModalActive &&
      !activeConnector &&
      targetTypes.includes(connectorType)
    ) {
      const [current] = connectorType === 'source' ? sources : sinks;
      if (current) setActiveConnector(current);
    }
  }, [activeConnector, connectorType, isModalActive, sinks, sources]);

  const handleModalClose = () => {
    setIsModalActive(false);
    setActiveConnector(null);
  };

  const handleConfirm = () => {
    modalChild.current.update();
    if (modalName === 'topics') {
      handleModalClose();
    }
  };

  const handleTrSelect = name => {
    const currentConnector = [...sources, ...sinks].find(
      connector => connector.className === name,
    );
    setActiveConnector(currentConnector);
  };

  const { pipelineName } = match.params;
  const pipelineGroup = `${workerClusterName}${pipelineName}`;
  const { ftpSource } = PIPELINES.CONNECTOR_TYPES;

  const getModalTitle = () => {
    switch (modalName) {
      case 'streams':
        return 'Add a new stream app';
      case 'topics':
        return 'Add a new topic';
      default:
        return `Add a new ${connectorType} connector`;
    }
  };

  const getModalTestId = () => {
    switch (modalName) {
      case 'streams':
        return 'streamapp-modal';
      case 'topics':
        return 'topic-modal';
      default:
        return `${connectorType}-connector-modal`;
    }
  };

  const dialogWidth = ['topics', 'streams'].includes(modalName) ? 'xs' : 'md';

  return (
    <ToolbarWrapper>
      <Dialog
        title={getModalTitle()}
        open={isModalActive}
        maxWidth={dialogWidth}
        handleConfirm={handleConfirm}
        handleClose={handleModalClose}
        confirmDisabled={isAddBtnDisabled}
      >
        <div data-testid={getModalTestId()}>
          {modalName === 'streams' && (
            <PipelineNewStream
              {...props}
              ref={modalChild}
              activeConnector={activeConnector}
              enableAddButton={setIsAddBtnDisabled}
              handleClose={handleModalClose}
              pipelineGroup={pipelineGroup}
            />
          )}

          {modalName === 'topics' && (
            <PipelineNewTopic
              ref={modalChild}
              updateGraph={updateGraph}
              graph={graph}
              topics={topics}
              enableAddButton={setIsAddBtnDisabled}
              workerClusterName={workerClusterName}
              pipelineName={pipelineName}
              handleConfirm={handleConfirm}
            />
          )}

          {['sources', 'sinks'].includes(modalName) && (
            <PipelineNewConnector
              ref={modalChild}
              connectorType={connectorType}
              connectors={modalName === 'sources' ? sources : sinks}
              activeConnector={activeConnector}
              onSelect={handleTrSelect}
              updateGraph={updateGraph}
              graph={graph}
              enableAddButton={setIsAddBtnDisabled}
              workerClusterName={workerClusterName}
              handleClose={handleModalClose}
              pipelineGroup={pipelineGroup}
            />
          )}
        </div>
      </Dialog>

      <Tooltip title="Add a source connector" enterDelay={1000}>
        <Icon
          className="fas fa-file-import"
          onClick={() => handleModalOpen('sources', 'source')}
          data-id={ftpSource}
          data-testid="toolbar-sources"
        />
      </Tooltip>

      <Tooltip title="Add a topic" enterDelay={1000}>
        <Icon
          className="fas fa-list-ul"
          onClick={() => handleModalOpen('topics', 'topic')}
          data-id={'topics'}
          data-testid="toolbar-topics"
        />
      </Tooltip>

      <Tooltip title="Add a stream app" enterDelay={1000}>
        <Icon
          className="fas fa-wind"
          onClick={() => handleModalOpen('streams', 'stream')}
          data-id={'streams'}
          data-testid="toolbar-streams"
        />
      </Tooltip>

      <Tooltip title="Add a sink connector" enterDelay={1000}>
        <Icon
          className="fas fa-file-export"
          onClick={() => handleModalOpen('sinks', 'sink')}
          data-id={ftpSource}
          data-testid="toolbar-sinks"
        />
      </Tooltip>

      <FileSavingStatus>
        {hasChanges ? 'Saving...' : 'All changes saved'}
      </FileSavingStatus>
    </ToolbarWrapper>
  );
};

PipelineToolbar.propTypes = {
  match: PropTypes.shape({
    params: PropTypes.shape({
      pipelineName: PropTypes.string.isRequired,
    }),
  }).isRequired,
  connectors: PropTypes.arrayOf(
    PropTypes.shape({
      className: PropTypes.string.isRequired,
      definitions: PropTypes.arrayOf(
        PropTypes.shape({
          displayName: PropTypes.string.isRequired,
          defaultValue: PropTypes.any,
        }),
      ).isRequired,
    }).isRequired,
  ).isRequired,
  graph: PropTypes.arrayOf(graphPropType).isRequired,
  updateGraph: PropTypes.func.isRequired,
  hasChanges: PropTypes.bool.isRequired,
  topics: PropTypes.array.isRequired,
  isLoading: PropTypes.bool.isRequired,
  workerClusterName: PropTypes.string.isRequired,
  brokerClusterName: PropTypes.string.isRequired,
};

export default PipelineToolbar;
