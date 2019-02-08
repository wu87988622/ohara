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
import DocumentTitle from 'react-document-title';
import styled from 'styled-components';
import toastr from 'toastr';
import { Link } from 'react-router-dom';
import { get, isEmpty } from 'lodash';

import * as MESSAGES from 'constants/messages';
import { TableLoader } from 'common/Loader';
import { Modal, ConfirmModal } from 'common/Modal';
import { DataTable } from 'common/Table';
import { Box } from 'common/Layout';
import { Warning } from 'common/Messages';
import { H2 } from 'common/Headings';
import { Button, Select } from 'common/Form';
import { primaryBtn } from 'theme/btnTheme';
import { PIPELINE } from 'constants/documentTitles';
import {
  addPipelineStatus,
  getEditUrl,
} from './pipelineUtils/pipelineListPageUtils';
import { fetchWorkers } from 'apis/workerApis';
import {
  fetchPipelines,
  createPipeline,
  deletePipeline,
} from 'apis/pipelinesApis';

const Wrapper = styled.div`
  padding-top: 75px;
  max-width: 1200px;
  width: calc(100% - 100px);
  margin: auto;
`;

const TopWrapper = styled.div`
  margin-bottom: 20px;
  display: flex;
  align-items: center;
`;

const NewPipelineBtn = styled(Button)`
  margin-left: auto;
`;

NewPipelineBtn.displayName = 'NewPipelineBtn';

const Table = styled(DataTable)`
  text-align: center;

  .is-running {
    background: ${props => props.theme.trBgColor};
  }
`;

Table.displayName = 'Table';

const LinkIcon = styled(Link)`
  color: ${props => props.theme.lightBlue};

  &:hover {
    color: ${props => props.theme.blue};
  }
`;

LinkIcon.displayName = 'LinkIcon';

const DeleteIcon = styled.button`
  color: ${props => props.theme.lightBlue};
  border: 0;
  font-size: 20px;
  cursor: pointer;
  background-color: transparent;

  &:hover {
    color: ${props => props.theme.red};
  }
`;

DeleteIcon.displayName = 'DeleteIcon';

const Inner = styled.div`
  padding: 30px 20px;
`;

class PipelineListPage extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      isExact: PropTypes.bool,
      params: PropTypes.object,
      path: PropTypes.string,
      url: PropTypes.string,
    }).isRequired,
  };

  headers = ['name', 'cluster', 'status', 'edit', 'delete'];
  state = {
    isSelectClusterModalActive: false,
    isDeletePipelineModalActive: false,
    isLoading: true,
    pipelines: [],
    workers: [],
    currWorker: {},
  };

  componentDidMount() {
    this.fetchPipelines();
    this.fetchWorkers();
  }

  fetchWorkers = async () => {
    const res = await fetchWorkers();
    const workers = get(res, 'data.result', null);

    if (workers) {
      this.setState({ workers, currWorker: workers[0] });
    }
  };

  fetchPipelines = async () => {
    const res = await fetchPipelines();
    const result = get(res, 'data.result', null);
    this.setState({ isLoading: false });

    if (result) {
      const pipelines = addPipelineStatus(result);
      this.setState({ pipelines });
    }
  };

  handleSelectChange = ({ target }) => {
    const selectedIdx = target.options.selectedIndex;
    const { id } = target.options[selectedIdx].dataset;

    this.setState({
      currWorker: {
        name: target.value,
        id,
      },
    });
  };

  handleSelectClusterModalClose = () => {
    this.setState(({ workers }) => {
      return {
        isSelectClusterModalActive: false,
        currWorker: workers[0],
      };
    });
  };

  handleSelectClusterModalOpen = e => {
    e.preventDefault();
    this.setState({ isSelectClusterModalActive: true });

    if (isEmpty(this.state.workers)) {
      toastr.error(MESSAGES.NO_WORKER_CLUSTER_FOUND_ERROR);
    }
  };

  handleSelectClusterModalConfirm = async () => {
    const { history, match } = this.props;
    const { currWorker } = this.state;

    const params = {
      name: 'Untitled pipeline',
      rules: {},
      cluster: currWorker.name,
    };
    const res = await createPipeline(params);
    const pipelineId = get(res, 'data.result.id', null);

    if (pipelineId) {
      this.handleSelectClusterModalClose();
      toastr.success(MESSAGES.PIPELINE_CREATION_SUCCESS);
      history.push(`${match.url}/new/${pipelineId}`);
    }
  };

  handleDeletePipelineModalOpen = id => {
    this.setState({
      isDeletePipelineModalActive: true,
      deletePipelineId: id,
    });
  };

  handleDeletePipelineModalClose = () => {
    this.setState({
      isDeletePipelineModalActive: false,
      deletePipelineId: '',
    });
  };

  handleDeletePipelineConfirm = async () => {
    const { deletePipelineId: id } = this.state;
    const res = await deletePipeline(id);
    const deletedId = get(res, 'data.result.id', null);
    const deletedPipeline = get(res, 'data.result.name', null);

    if (deletedId) {
      this.setState(({ pipelines }) => {
        const _pipelines = pipelines.filter(p => p.id !== deletedId);
        return {
          pipelines: _pipelines,
          isDeletePipelineModalActive: false,
          deletePipelineId: '',
        };
      });
      toastr.success(
        `${MESSAGES.PIPELINE_DELETION_SUCCESS} ${deletedPipeline}`,
      );
    } else {
      toastr.error(`${MESSAGES.PIPELINE_DELETION_ERROR} ${deletedPipeline}`);
    }
  };

  render() {
    const { match } = this.props;
    const {
      isDeletePipelineModalActive,
      isSelectClusterModalActive,
      pipelines,
      isLoading,
      workers,
      currWorker,
    } = this.state;

    return (
      <DocumentTitle title={PIPELINE}>
        <React.Fragment>
          <Modal
            isActive={isSelectClusterModalActive}
            title="Select cluster"
            width="370px"
            confirmBtnText="Next"
            handleConfirm={this.handleSelectClusterModalConfirm}
            handleCancel={this.handleSelectClusterModalClose}
            isConfirmDisabled={isEmpty(workers) ? true : false}
          >
            <Inner>
              <Warning text="Please select a cluster for the new pipeline" />
              <Select
                isObject
                list={workers}
                selected={currWorker}
                handleChange={this.handleSelectChange}
              />
            </Inner>
          </Modal>

          <ConfirmModal
            isActive={isDeletePipelineModalActive}
            title="Delete pipeline?"
            confirmBtnText="Yes, Delete this pipeline"
            cancelBtnText="No, Keep it"
            handleCancel={this.handleDeletePipelineModalClose}
            handleConfirm={this.handleDeletePipelineConfirm}
            message="Are you sure you want to delete this pipeline? This action cannot be redo!"
            isDelete
          />

          <Wrapper>
            <TopWrapper>
              <H2>Pipelines</H2>
              <NewPipelineBtn
                theme={primaryBtn}
                text="New pipeline"
                data-testid="new-pipeline"
                handleClick={this.handleSelectClusterModalOpen}
              />
            </TopWrapper>
            <Box>
              {isLoading ? (
                <TableLoader />
              ) : (
                <Table headers={this.headers}>
                  {pipelines.map(pipeline => {
                    const { id, name, status, workerClusterName } = pipeline;
                    const isRunning = status === 'Running' ? true : false;
                    const trCls = isRunning ? 'is-running' : '';
                    const editUrl = getEditUrl(pipeline, match);

                    return (
                      <tr key={id} className={trCls}>
                        <td>{name}</td>
                        <td>{workerClusterName}</td>
                        <td>{status}</td>
                        <td className="has-icon">
                          <LinkIcon to={editUrl}>
                            <i className="far fa-edit" />
                          </LinkIcon>
                        </td>
                        <td data-testid="delete-pipeline" className="has-icon">
                          <DeleteIcon
                            onClick={() =>
                              this.handleDeletePipelineModalOpen(id)
                            }
                          >
                            <i className="far fa-trash-alt" />
                          </DeleteIcon>
                        </td>
                      </tr>
                    );
                  })}
                </Table>
              )}
            </Box>
          </Wrapper>
        </React.Fragment>
      </DocumentTitle>
    );
  }
}

export default PipelineListPage;
