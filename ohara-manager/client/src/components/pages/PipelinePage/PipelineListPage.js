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

import React, { useState } from 'react';
import PropTypes from 'prop-types';
import DocumentTitle from 'react-document-title';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import { Link } from 'react-router-dom';
import { get, isEmpty, isUndefined } from 'lodash';
import DialogContent from '@material-ui/core/DialogContent';
import { Form, Field } from 'react-final-form';

import * as MESSAGES from 'constants/messages';
import * as utils from './pipelineUtils/pipelineListPageUtils';
import * as URLS from 'constants/urls';
import { ListLoader } from 'components/common/Loader';
import { DeleteDialog } from 'components/common/Mui/Dialog';
import { Warning } from 'components/common/Messages';
import { H2 } from 'components/common/Headings';
import { PIPELINE } from 'constants/documentTitles';
import * as s from './styles';
import { SortTable } from 'components/common/Mui/Table';
import useSnackbar from 'components/context/Snackbar/useSnackbar';
import * as useApi from 'components/controller';
import * as URL from 'components/controller/url';
import { Dialog } from 'components/common/Mui/Dialog';
import { InputField, Select } from 'components/common/Mui/Form';

const PipelineListPage = props => {
  const { match } = props;
  const [isSelectClusterModalActive, setIsSelectClusterModalActive] = useState(
    false,
  );
  const [
    isDeletePipelineModalActive,
    setIsDeletePipelineModalActive,
  ] = useState(false);
  const [isNewPipelineWorking, setIsNewPipelineWorking] = useState(false);
  const [isDeletePipelineWorking, setIsDeletePipelineWorking] = useState(false);
  const [pipelineToBeDeleted, setPipelineToBeDeleted] = useState('');
  const { showMessage } = useSnackbar();
  const {
    data: pipelines,
    isLoading: isFetchingPipeline,
    refetch: refetchPipelines,
  } = useApi.useFetchApi(URL.PIPELINE_URL);
  const { data: workers, isLoading: isFetchingWorker } = useApi.useFetchApi(
    URL.WORKER_URL,
  );
  const { getData: pipelineRes, postApi: createPipeline } = useApi.usePostApi(
    URL.PIPELINE_URL,
  );
  const {
    getData: deletePipelineRes,
    deleteApi: deletePipeline,
  } = useApi.useDeleteApi(URL.PIPELINE_URL);

  const handleSelectClusterModalClose = () => {
    setIsSelectClusterModalActive(false);
  };

  const handleSelectClusterModalOpen = () => {
    setIsSelectClusterModalActive(true);
  };

  const handleSelectClusterModalConfirm = async values => {
    const { history, match } = props;
    const params = {
      name: values.name,
      rules: {},
      workerClusterName: values.workspace,
    };

    setIsNewPipelineWorking(true);
    await createPipeline(params);
    setIsNewPipelineWorking(false);
    const pipelineName = get(pipelineRes(), 'data.result.name', null);

    if (pipelineName) {
      handleSelectClusterModalClose();
      showMessage(MESSAGES.PIPELINE_CREATION_SUCCESS);
      history.push(`${match.url}/new/${pipelineName}`);
    }
  };

  const handleDeletePipelineModalOpen = name => {
    setIsDeletePipelineModalActive(true);
    setPipelineToBeDeleted(name);
  };

  const handleDeletePipelineModalClose = () => {
    setIsDeletePipelineModalActive(false);
    setPipelineToBeDeleted('');
  };

  const handleDeletePipelineConfirm = async () => {
    setIsDeletePipelineWorking(true);
    await deletePipeline(pipelineToBeDeleted);
    const isSuccess = get(deletePipelineRes(), 'data.isSuccess', false);
    setIsDeletePipelineWorking(false);

    if (isSuccess) {
      refetchPipelines();
      showMessage(
        `${MESSAGES.PIPELINE_DELETION_SUCCESS} ${pipelineToBeDeleted}`,
      );
    } else {
      showMessage(`${MESSAGES.PIPELINE_DELETION_ERROR} ${pipelineToBeDeleted}`);
    }
    setIsDeletePipelineModalActive(false);
  };

  const deleteButton = pipeline => {
    const { name } = pipeline;
    return (
      <Tooltip title={`Delete ${name} pipeline`} enterDelay={1000}>
        <IconButton
          onClick={() => handleDeletePipelineModalOpen(name)}
          data-testid="delete-pipeline"
        >
          <s.StyledIcon className="fas fa-trash-alt" />
        </IconButton>
      </Tooltip>
    );
  };

  const editButton = pipeline => {
    const { name } = pipeline;
    return (
      <Tooltip title={`Edit ${name} pipeline`} enterDelay={1000}>
        <Link to={utils.getEditUrl(pipeline, match)}>
          <IconButton data-testid="edit-pipeline">
            <s.StyledIcon className="fas fa-external-link-square-alt" />
          </IconButton>
        </Link>
      </Tooltip>
    );
  };

  const headRows = [
    { id: 'name', label: 'Name' },
    { id: 'workspace', label: 'Workspace' },
    { id: 'status', label: 'Status' },
    { id: 'edit', label: 'Edit', sortable: false },
    { id: 'delete', label: 'Delete', sortable: false },
  ];

  const rows = utils
    .addPipelineStatus(get(pipelines, 'data.result', []))
    .map(pipeline => {
      return {
        name: pipeline.name,
        workspace: pipeline.workerClusterName,
        status: pipeline.status,
        edit: editButton(pipeline),
        delete: deleteButton(pipeline),
      };
    });
  return (
    <Form
      onSubmit={handleSelectClusterModalConfirm}
      initialValues={{}}
      render={({ handleSubmit, submitting, values }) => {
        return (
          <DocumentTitle title={PIPELINE}>
            <>
              <Dialog
                title="New pipeline"
                handleConfirm={handleSubmit}
                isLoading={isNewPipelineWorking}
                confirmDisabled={
                  submitting ||
                  isUndefined(values.workspace) ||
                  isUndefined(values.name)
                }
                open={isSelectClusterModalActive}
                handelClose={handleSelectClusterModalClose}
                testId="new-pipeline-modal"
              >
                {isFetchingWorker ? (
                  <s.LoaderWrapper>
                    <ListLoader />
                  </s.LoaderWrapper>
                ) : (
                  <>
                    {isEmpty(get(workers, 'data.result')) ? (
                      <DialogContent>
                        <Warning
                          text={
                            <>
                              It seems like you haven't created any worker
                              clusters yet. You can create one from
                              <Link to={URLS.WORKSPACES}> here</Link>
                            </>
                          }
                        />
                      </DialogContent>
                    ) : (
                      <form onSubmit={handleSubmit}>
                        <DialogContent>
                          <Field
                            name="name"
                            label="Pipeline name"
                            placeholder="PipelineName"
                            inputProps={{ 'data-testid': 'name-input' }}
                            component={InputField}
                            autoFocus
                          />
                        </DialogContent>
                        <DialogContent>
                          <Field
                            name="workspace"
                            label="Workspace name"
                            data-testid="cluster-select"
                            list={get(workers, 'data.result', []).map(
                              worker => worker.name,
                            )}
                            component={Select}
                          />
                        </DialogContent>
                      </form>
                    )}
                  </>
                )}
              </Dialog>

              <DeleteDialog
                title="Delete pipeline?"
                content={`Are you sure you want to delete the pipeline: ${pipelineToBeDeleted}? This action cannot be undone!`}
                open={isDeletePipelineModalActive}
                working={isDeletePipelineWorking}
                handleConfirm={handleDeletePipelineConfirm}
                handleClose={handleDeletePipelineModalClose}
              />

              <s.Wrapper>
                <s.TopWrapper>
                  <H2>Pipelines</H2>
                  <s.NewPipelineBtn
                    text="New pipeline"
                    testId="new-pipeline"
                    onClick={handleSelectClusterModalOpen}
                  />
                </s.TopWrapper>
                <SortTable
                  isLoading={isFetchingPipeline}
                  headRows={headRows}
                  rows={rows}
                  tableName="pipeline"
                />
              </s.Wrapper>
            </>
          </DocumentTitle>
        );
      }}
    />
  );
};

PipelineListPage.propTypes = {
  match: PropTypes.shape({
    url: PropTypes.string.isRequired,
  }).isRequired,
  history: PropTypes.shape({
    push: PropTypes.func.isRequired,
  }).isRequired,
};

export default PipelineListPage;
