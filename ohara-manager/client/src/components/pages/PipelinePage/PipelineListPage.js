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
import toastr from 'toastr';
import { Link } from 'react-router-dom';
import { get, isEmpty } from 'lodash';

import * as MESSAGES from 'constants/messages';
import * as utils from './pipelineUtils/pipelineListPageUtils';
import * as pipelineApi from 'api/pipelineApi';
import * as URLS from 'constants/urls';
import { TableLoader, ListLoader } from 'components/common/Loader';
import { Modal, ConfirmModal } from 'components/common/Modal';
import { Box } from 'components/common/Layout';
import { Warning } from 'components/common/Messages';
import { H2 } from 'components/common/Headings';
import { Select } from 'components/common/Form';
import { primaryBtn } from 'theme/btnTheme';
import { PIPELINE } from 'constants/documentTitles';
import { fetchWorkers } from 'api/workerApi';
import { Input, Label, FormGroup } from 'components/common/Form';
import {
  Wrapper,
  TopWrapper,
  NewPipelineBtn,
  Table,
  LinkIcon,
  DeleteIcon,
  Inner,
  LoaderWrapper,
} from './styles';

class PipelineListPage extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      url: PropTypes.string.isRequired,
    }).isRequired,
    history: PropTypes.shape({
      push: PropTypes.func.isRequired,
    }).isRequired,
  };

  headers = ['name', 'service', 'status', 'edit', 'delete'];
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
    newPipelineName: '',
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
    const res = await pipelineApi.fetchPipelines();
    const result = get(res, 'data.result', null);
    this.setState({ isFetchingPipeline: false });

    if (result) {
      const pipelines = utils.addPipelineStatus(result);
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
    const { currWorker, newPipelineName } = this.state;

    const params = {
      name: newPipelineName,
      rules: {},
      workerClusterName: currWorker.name,
    };

    this.setState({ isNewPipelineWorking: true });
    const res = await pipelineApi.createPipeline(params);
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

  handleChange = ({ target: { value } }) => {
    this.setState({ newPipelineName: value });
  };

  handleDeletePipelineConfirm = async () => {
    const { deletePipelineId: id, pipelineName: name } = this.state;
    this.setState({ isDeletePipelineWorking: true });
    const res = await pipelineApi.deletePipeline(id);
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
      newPipelineName,
      workers,
      currWorker,
    } = this.state;
    return (
      <DocumentTitle title={PIPELINE}>
        <>
          <Modal
            isActive={isSelectClusterModalActive}
            title="New pipeline"
            width="370px"
            confirmBtnText="Add"
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
                {isEmpty(workers) ? (
                  <Warning
                    text={
                      <>
                        It seems like you haven't created any worker clusters
                        yet. You can create one from
                        <Link to={URLS.WORKSPACES}> here</Link>
                      </>
                    }
                  />
                ) : (
                  <>
                    <FormGroup data-testid="name">
                      <Label htmlFor="pipelineInput">Pipeline name</Label>
                      <Input
                        id="pipelineInput"
                        name="name"
                        width="100%"
                        placeholder="Pipeline name"
                        data-testid="name-input"
                        value={newPipelineName}
                        handleChange={this.handleChange}
                      />
                    </FormGroup>
                    <FormGroup data-testid="workerSelect">
                      <Label htmlFor="workerSelect">Worker cluster name</Label>
                      <Select
                        id="workerSelect"
                        data-testid="cluster-select"
                        list={workers}
                        selected={currWorker}
                        handleChange={this.handleSelectChange}
                        isObject
                      />
                    </FormGroup>
                  </>
                )}
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
                    const editUrl = utils.getEditUrl(pipeline, match);

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
        </>
      </DocumentTitle>
    );
  }
}

export default PipelineListPage;
