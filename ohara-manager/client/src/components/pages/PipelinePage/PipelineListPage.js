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
import { TableLoader, ListLoader } from 'common/Loader';
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
import { fetchWorkers } from 'api/workerApi';
import {
  fetchPipelines,
  createPipeline,
  deletePipeline,
} from 'api/pipelineApi';

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

const LoaderWrapper = styled.div`
  margin: 30px;
`;

class PipelineListPage extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      url: PropTypes.string.isRequired,
    }).isRequired,
    history: PropTypes.shape({
      push: PropTypes.func.isRequired,
    }).isRequired,
  };

  headers = ['name', 'cluster', 'status', 'edit', 'delete'];
  state = {
    isSelectClusterModalActive: false,
    isDeletePipelineModalActive: false,
    isFetchingPipeline: true,
    isFetchingWorker: true,
    pipelines: [],
    workers: [],
    currWorker: {},
    isNewPipelineWorking: false,
    isDeletePipelineWorking: false,
  };

  componentDidMount() {
    this.fetchPipelines();
    this.fetchWorkers();
  }

  fetchWorkers = async () => {
    const res = await fetchWorkers();
    const workers = get(res, 'data.result', null);
    this.setState({ isFetchingWorker: false });

    if (workers) {
      this.setState({ workers, currWorker: workers[0] });
    }
  };

  fetchPipelines = async () => {
    const res = await fetchPipelines();
    const result = get(res, 'data.result', null);
    this.setState({ isFetchingPipeline: false });

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
  };

  handleSelectClusterModalConfirm = async () => {
    const { history, match } = this.props;
    const { currWorker } = this.state;

    const params = {
      name: 'Untitled pipeline',
      rules: {},
      workerClusterName: currWorker.name,
    };

    this.setState({ isNewPipelineWorking: true });
    const res = await createPipeline(params);
    this.setState({ isNewPipelineWorking: false });
    const pipelineId = get(res, 'data.result.id', null);

    if (pipelineId) {
      this.handleSelectClusterModalClose();
      toastr.success(MESSAGES.PIPELINE_CREATION_SUCCESS);
      history.push(`${match.url}/new/${pipelineId}`);
    }
  };

  handleDeletePipelineModalOpen = (id, name) => {
    this.setState({
      isDeletePipelineModalActive: true,
      deletePipelineId: id,
      pipelineName: name,
    });
  };

  handleDeletePipelineModalClose = () => {
    this.setState({
      isDeletePipelineModalActive: false,
      deletePipelineId: '',
    });
  };

  handleDeletePipelineConfirm = async () => {
    const { deletePipelineId: id, pipelineName: name } = this.state;
    this.setState({ isDeletePipelineWorking: true });
    const res = await deletePipeline(id);
    const isSuccess = get(res, 'data.isSuccess', false);
    this.setState({ isDeletePipelineWorking: false });

    if (isSuccess) {
      this.setState(({ pipelines }) => {
        const _pipelines = pipelines.filter(p => p.id !== id);
        return {
          pipelines: _pipelines,
          isDeletePipelineModalActive: false,
          deletePipelineId: '',
        };
      });
      toastr.success(`${MESSAGES.PIPELINE_DELETION_SUCCESS} ${name}`);
    } else {
      toastr.error(`${MESSAGES.PIPELINE_DELETION_ERROR} ${name}`);
    }
  };

  render() {
    const { match } = this.props;
    const {
      isDeletePipelineModalActive,
      isSelectClusterModalActive,
      isFetchingPipeline,
      isFetchingWorker,
      isNewPipelineWorking,
      isDeletePipelineWorking,
      pipelines,
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
            isConfirmWorking={isNewPipelineWorking}
          >
            {isFetchingWorker ? (
              <LoaderWrapper>
                <ListLoader />
              </LoaderWrapper>
            ) : (
              <Inner>
                <Warning text="Please select a cluster for the new pipeline" />
                <Select
                  data-testid="cluster-select"
                  list={workers}
                  selected={currWorker}
                  handleChange={this.handleSelectChange}
                  isObject
                />
              </Inner>
            )}
          </Modal>

          <ConfirmModal
            isActive={isDeletePipelineModalActive}
            title="Delete pipeline?"
            confirmBtnText="Yes, Delete this pipeline"
            cancelBtnText="No, Keep it"
            handleCancel={this.handleDeletePipelineModalClose}
            handleConfirm={this.handleDeletePipelineConfirm}
            isConfirmWorking={isDeletePipelineWorking}
            message="Are you sure you want to delete this pipeline? This action cannot be undone!"
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
              {isFetchingPipeline ? (
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
                        <td data-testid="edit-pipeline" className="has-icon">
                          <LinkIcon to={editUrl}>
                            <i className="far fa-edit" />
                          </LinkIcon>
                        </td>
                        <td data-testid="delete-pipeline" className="has-icon">
                          <DeleteIcon
                            onClick={() =>
                              this.handleDeletePipelineModalOpen(id, name)
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
