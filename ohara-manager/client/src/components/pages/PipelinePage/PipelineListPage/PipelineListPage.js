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

import React, { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import DocumentTitle from 'react-document-title';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import DialogContent from '@material-ui/core/DialogContent';
import { Link } from 'react-router-dom';
import { get, isEmpty, isUndefined } from 'lodash';
import { Form, Field } from 'react-final-form';

import * as pipelineApi from 'api/pipelineApi';
import * as connectorApi from 'api/connectorApi';
import * as workerApi from 'api/workerApi';
import * as streamApi from 'api/streamApi';
import * as MESSAGES from 'constants/messages';
import * as utils from './pipelineListPageUtils';
import * as URLS from 'constants/urls';
import * as useApi from 'components/controller';
import * as s from '../styles';
import * as URL from 'components/controller/url';
import useSnackbar from 'components/context/Snackbar/useSnackbar';
import validate from '../validate';
import { ListLoader } from 'components/common/Loader';
import { DeleteDialog } from 'components/common/Mui/Dialog';
import { Warning } from 'components/common/Messages';
import { H2 } from 'components/common/Headings';
import { PIPELINE } from 'constants/documentTitles';
import { SortTable } from 'components/common/Mui/Table';
import { Dialog } from 'components/common/Mui/Dialog';
import { InputField, Select } from 'components/common/Mui/Form';
import { Progress } from 'components/common/Mui/Feedback';

const PipelineListPage = props => {
  const [isNewModalOpen, setIsNewModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isNewModalWorking, setIsNewModalWorking] = useState(false);
  const [isDeleteModalWorking, setIsDeleteModalWorking] = useState(false);
  const [pipelineToBeDeleted, setPipelineToBeDeleted] = useState('');
  const [steps, setSteps] = useState([]);
  const [activeStep, setActiveStep] = useState(0);
  const [pipelines, setPipelines] = useState([]);
  const [workers, setWorkers] = useState([]);
  const [isFetchingPipeline, setIsFetchingPipeline] = useState(false);
  const [isFetchingWorker, setIsFetchingWorker] = useState(false);

  const { showMessage } = useSnackbar();
  const { waitApi } = useApi.useWaitApi();

  const fetchPipelines = useCallback(async () => {
    setIsFetchingPipeline(true);
    const response = await pipelineApi.fetchPipelines();
    const pipelines = get(response, 'data.result', []);
    setIsFetchingPipeline(false);
    setPipelines(pipelines);
  }, []);

  useEffect(() => {
    fetchPipelines();
  }, [fetchPipelines]);

  useEffect(() => {
    const fetchWorkers = async () => {
      setIsFetchingWorker(true);
      const response = await workerApi.fetchWorkers();
      const workers = get(response, 'data.result', []);
      setIsFetchingWorker(false);
      setWorkers(workers);
    };

    fetchWorkers();
  }, []);

  const handleNewModalSubmit = async values => {
    const { history, match } = props;
    const { name: pipelineName, workspace: workerClusterName } = values;

    const params = {
      name: values.name,
      group: `${workerClusterName}${pipelineName}`,
      tags: {
        workerClusterName,
      },
    };

    setIsNewModalWorking(true);
    const response = await pipelineApi.createPipeline(params);
    const updatedPipelineName = get(response, 'data.result.name', null);
    setIsNewModalWorking(false);

    if (updatedPipelineName) {
      setIsNewModalOpen(false);
      showMessage(MESSAGES.PIPELINE_CREATION_SUCCESS);
      history.push(
        `${match.url}/edit/${workerClusterName}/${updatedPipelineName}`,
      );
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

  const deleteObjects = async (group, objects) => {
    const runningObjects = objects.filter(
      object => object.kind !== 'topic' && Boolean(object.state),
    );

    // If there's no running objects, don't use steps
    if (!isEmpty(runningObjects)) {
      setSteps([...objects.map(object => object.name)]);
    }

    // Need to use a while loop so we can update
    // react state: `activeStep` in the loop
    let index = 0;
    while (index < objects.length) {
      const object = objects[index];
      const { kind, name: objectName } = object;
      const isRunning = !isUndefined(object.state);
      const isConnector = kind === 'source' || kind === 'sink';

      // Connectors and stream apps are the only services that
      // we're going to delete. Topics, on the other hand, should
      // be deleted in the workspace not pipeline for now!
      if (isConnector) {
        if (isRunning) {
          await connectorApi.stopConnector(group, objectName);
          await waitApi({
            url: `${URL.CONNECTOR_URL}/${objectName}?group=${group}`,
            checkFn: response => isUndefined(response.data.result.state),
          });
        }
        await connectorApi.deleteConnector(group, objectName);
      }

      if (kind === 'stream') {
        if (isRunning) {
          await streamApi.stopStreamApp(group, objectName);
          await waitApi({
            url: `${URL.STREAM_URL}/${objectName}?group=${group}`,
            checkFn: response => isUndefined(response.data.result.state),
          });
        }
        await streamApi.deleteProperty(group, objectName);
      }

      index++;
      setActiveStep(index);
    }
  };

  const handleDeleteConfirm = async () => {
    setIsDeleteModalWorking(true);
    const [{ name, objects, group }] = pipelines.filter(
      pipeline => pipeline.name === pipelineToBeDeleted,
    );
    // First, stop running objects then delete them
    await deleteObjects(group, objects);

    // Second, update pipeline flows, so everthing is relased from this pipeline
    await pipelineApi.updatePipeline({
      name,
      group,
      params: {
        flows: [],
      },
    });

    // Finally, let's delete the pipeline
    const response = await pipelineApi.deletePipeline(group, name);
    const isSuccess = get(response, 'data.isSuccess', false);

    setIsDeleteModalWorking(false);

    if (isSuccess) {
      setIsDeleteModalOpen(false);
      const response = await pipelineApi.fetchPipelines();
      const pipelines = get(response, 'data.result', []);
      setPipelines(pipelines);

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
    const {
      name,
      tags: { workerClusterName },
    } = pipeline;

    const url = `${props.match.url}/edit/${workerClusterName}/${name}`;

    return (
      <Tooltip title={`Edit ${name} pipeline`} enterDelay={1000}>
        <Link to={url}>
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

  const rows = utils.addPipelineStatus(pipelines).map(pipeline => {
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
                {isFetchingWorker || isFetchingPipeline ? (
                  <s.LoaderWrapper>
                    <ListLoader />
                  </s.LoaderWrapper>
                ) : (
                  <>
                    {isEmpty(workers) ? (
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
                            list={workers.map(worker => worker.name)}
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
                handleConfirm={handleDeleteConfirm}
                handleClose={handleDeleteClose}
              />

              {// Display the progress when deleting if there are running objects
              !isEmpty(steps) && (
                <Progress
                  open={isDeleteModalWorking}
                  createTitle={`Deleting pipeline ${pipelineToBeDeleted}`}
                  steps={steps}
                  activeStep={activeStep}
                />
              )}
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
