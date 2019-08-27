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
import DialogContent from '@material-ui/core/DialogContent';
import { Link } from 'react-router-dom';
import { get, isEmpty } from 'lodash';
import { Form, Field } from 'react-final-form';

import * as MESSAGES from 'constants/messages';
import * as utils from './pipelineUtils/pipelineListPageUtils';
import * as URLS from 'constants/urls';
import * as useApi from 'components/controller';
import * as s from './styles';
import * as URL from 'components/controller/url';
import useSnackbar from 'components/context/Snackbar/useSnackbar';
import validate from './validate';
import { ListLoader } from 'components/common/Loader';
import { DeleteDialog } from 'components/common/Mui/Dialog';
import { Warning } from 'components/common/Messages';
import { H2 } from 'components/common/Headings';
import { PIPELINE } from 'constants/documentTitles';
import { SortTable } from 'components/common/Mui/Table';
import { Dialog } from 'components/common/Mui/Dialog';
import { InputField, Select } from 'components/common/Mui/Form';

const PipelineListPage = props => {
  const { match } = props;
  const [isNewModalOpen, setIsNewModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isNewModalWorking, setisNewModalWorking] = useState(false);
  const [isDeleteModalWorking, setisDeleteModalWorking] = useState(false);
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

  const handleNewModalSubmit = async values => {
    const { history, match } = props;
    const params = {
      name: values.name,
      tags: {
        workerClusterName: values.workspace,
      },
    };

    setisNewModalWorking(true);
    await createPipeline(params);
    setisNewModalWorking(false);
    const pipelineName = get(pipelineRes(), 'data.result.name', null);

    if (pipelineName) {
      setIsNewModalOpen(false);
      showMessage(MESSAGES.PIPELINE_CREATION_SUCCESS);
      history.push(`${match.url}/new/${pipelineName}`);
    }
  };

  const handleDeletePipelineModalOpen = name => {
    setIsDeleteModalOpen(true);
    setPipelineToBeDeleted(name);
  };

  const handleDeleteClose = () => {
    setIsDeleteModalOpen(false);
    setPipelineToBeDeleted('');
  };

  const handleDeleteConfirm = async () => {
    setisDeleteModalWorking(true);
    await deletePipeline(pipelineToBeDeleted);
    const isSuccess = get(deletePipelineRes(), 'data.isSuccess', false);
    setisDeleteModalWorking(false);

    if (isSuccess) {
      refetchPipelines();
      setIsDeleteModalOpen(false);
      showMessage(
        `${MESSAGES.PIPELINE_DELETION_SUCCESS} ${pipelineToBeDeleted}`,
      );
    }
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
        workspace: pipeline.tags.workerClusterName,
        status: pipeline.status,
        edit: editButton(pipeline),
        delete: deleteButton(pipeline),
      };
    });
  return (
    <DocumentTitle title={PIPELINE}>
      <Form
        onSubmit={handleNewModalSubmit}
        initialValues={{}}
        validate={validate}
        render={({ handleSubmit, form, submitting, invalid }) => {
          return (
            <>
              <s.Wrapper>
                <s.TopWrapper>
                  <H2>Pipelines</H2>
                  <s.NewPipelineBtn
                    text="NEW PIPELINE"
                    testId="new-pipeline"
                    onClick={() => setIsNewModalOpen(true)}
                  />
                </s.TopWrapper>
                <SortTable
                  isLoading={isFetchingPipeline}
                  headRows={headRows}
                  rows={rows}
                  tableName="pipeline"
                />
              </s.Wrapper>

              <Dialog
                title="New pipeline"
                handleConfirm={handleSubmit}
                isLoading={isNewModalWorking}
                confirmDisabled={submitting || invalid}
                open={isNewModalOpen}
                handleClose={() => {
                  setIsNewModalOpen(false);
                  form.reset();
                }}
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
                      <form data-testid="form" onSubmit={handleSubmit}>
                        <DialogContent>
                          <Field
                            name="name"
                            label="Pipeline name"
                            placeholder="PipelineName"
                            component={InputField}
                            inputProps={{
                              'data-testid': 'pipeline-name-input',
                            }}
                            autoFocus
                            required
                          />
                        </DialogContent>
                        <DialogContent>
                          <Field
                            name="workspace"
                            label="Workspace name"
                            inputProps={{
                              'data-testid': 'workspace-name-select',
                            }}
                            list={get(workers, 'data.result', []).map(
                              worker => worker.name,
                            )}
                            component={Select}
                            required
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
                open={isDeleteModalOpen}
                working={isDeleteModalWorking}
                handleConfirm={handleDeleteConfirm}
                handleClose={handleDeleteClose}
              />
            </>
          );
        }}
      />
    </DocumentTitle>
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
