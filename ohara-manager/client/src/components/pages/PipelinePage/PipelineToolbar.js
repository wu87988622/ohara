import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import * as _ from 'utils/commonUtils';
import { Modal } from 'common/Modal';
import { DataTable } from 'common/Table';
import { update, fetchCluster } from 'utils/pipelineToolbarUtils';
import * as PIPELINES from 'constants/pipelines';
import {
  lightestBlue,
  lighterBlue,
  lightBlue,
  radiusNormal,
  durationNormal,
  trBgColor,
  blue,
} from 'theme/variables';
import PipelineNewStream from './PipelineNewStream';
import PipelineNewTopic from './PipelineNewTopic';

const ToolbarWrapper = styled.div`
  margin-bottom: 15px;
  padding: 15px 30px;
  border: 1px solid ${lightestBlue};
  border-radius: ${radiusNormal};
  display: flex;
  align-items: center;
`;

ToolbarWrapper.displayName = 'ToolbarWrapper';

const TableWrapper = styled.div`
  margin: 30px 30px 40px;
`;

const Table = styled(DataTable)`
  thead th {
    color: ${lightBlue};
    font-weight: normal;
  }

  td {
    color: ${lighterBlue};
  }

  tbody tr {
    cursor: pointer;
  }

  .is-active {
    background-color: ${trBgColor};
  }
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

const FileSavingStatus = styled.div`
  margin-left: auto;
  color: red;
  font-size: 12px;
  color: ${lighterBlue};
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
        uuid: PropTypes.string,
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

  update = () => {
    const { updateGraph, graph } = this.props;
    const { activeConnector: connector } = this.state;
    update({ graph, updateGraph, connector });
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
    // this.update();
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
    const { hasChanges } = this.props;
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

    const PipelineNewConnector = ({
      connectorType,
      connectors,
      activeConnector,
      onSelect,
    }) => {
      return (
        <TableWrapper>
          <Table headers={PIPELINES.TABLE_HEADERS}>
            {connectors.map(({ className: name, version, revision }) => {
              const isActive =
                name === activeConnector.className ? 'is-active' : '';
              return (
                <tr
                  className={isActive}
                  key={name}
                  onClick={() => onSelect(name)}
                >
                  <td>{name}</td>
                  <td>{version}</td>
                  <td>{revision}</td>
                </tr>
              );
            })}
          </Table>
        </TableWrapper>
      );
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
            <PipelineNewStream ref={this.modalChild} />
          )}

          {modalName === modalNames.ADD_TOPIC && (
            <PipelineNewTopic ref={this.modalChild} />
          )}

          {_.includes(
            [modalNames.ADD_SOURCE_CONNECTOR, modalNames.ADD_SINK_CONNECTOR],
            modalName,
          ) && (
            <PipelineNewConnector
              connectorType={connectorType}
              connectors={this.state[connectorType]}
              activeConnector={activeConnector}
              onSelect={this.handleTrSelect}
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
