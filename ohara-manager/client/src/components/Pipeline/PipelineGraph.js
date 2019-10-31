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

import React, { useEffect } from 'react';
import styled from 'styled-components';
import Typography from '@material-ui/core/Typography';
import { useHistory, useParams } from 'react-router-dom';

import { useWorkspace } from 'context/WorkspaceContext';
import { usePipeline } from 'context/PipelineContext';
import { useNewWorkspace } from 'context/NewWorkspaceContext';
import { FullScreenDialog } from 'components/common/Dialog';
import { Card } from 'components/common/Card';

const Container = styled.div`
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: center;

  .quick-mode {
    margin-right: ${props => props.theme.spacing(4)}px;
  }
`;

const PipelineGraph = () => {
  const history = useHistory();
  const { workspaces } = useWorkspace();
  const { pipelines, doFetch: fetchPipelines } = usePipeline();
  const { workspaceName, pipelineName } = useParams();
  const { isOpen: isDialogOpen, setIsOpen } = useNewWorkspace();

  const closeNewWorkspace = () => setIsOpen(false);

  useEffect(() => {
    fetchPipelines(workspaceName);
  }, [fetchPipelines, workspaceName]);

  const hasPipeline = pipelines.length > 0;
  let currentPipeline;

  if (pipelineName) {
    const current = pipelines.find(pipeline => pipeline.name === pipelineName);

    // If the `current` pipeline is found in the pipeline list
    if (current) currentPipeline = current;

    // If the `current` pipeline is not found in the list but the
    // list is not empty, let's display the first pipeline
    if (current === undefined && hasPipeline) {
      history.push(`/${workspaceName}/${pipelines[0].name}`);
    }
  } else if (hasPipeline) {
    history.push(`/${workspaceName}/${pipelines[0].name}`);
  }

  const hasWorkspace = workspaces.length > 0;

  let currentWorkspace;
  if (workspaceName) {
    const current = workspaces.find(
      workspace => workspace.settings.name === workspaceName,
    );

    // If the `current` workspace is found in the workspace list
    if (current) currentWorkspace = current;

    // If the `current` workspace is not found in the list but the
    // list is not empty, let's display the first workspace
    if (current === undefined && hasWorkspace) {
      history.push(`/${workspaces[0].settings.name}`);
    }
  } else if (hasWorkspace) {
    // Display the first workspace as the fallback
    history.push(`/${workspaces[0].settings.name}`);
  }

  if (isDialogOpen && currentWorkspace) {
    closeNewWorkspace();
  }

  return currentWorkspace ? (
    <>
      {currentPipeline ? (
        <h1>Current pipeline : {pipelineName}</h1>
      ) : (
        <Typography gutterBottom variant="h5" component="h2">
          {`You don't have any pipeline in ${workspaceName} yet! Click here to create one!`}
        </Typography>
      )}
    </>
  ) : (
    <>
      <Typography gutterBottom variant="h5" component="h2">
        You don't have any workspace yet! Click here to create one!
      </Typography>
      <FullScreenDialog open={isDialogOpen} handleClose={closeNewWorkspace}>
        <Container>
          <Card
            className="quick-mode card-item"
            title="Quick"
            description="Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book."
          />
          <Card
            className="expert-mode card-item"
            title="Expert"
            description="It is a long established fact that a reader will be distracted by the readable content of a page when looking at its layout. The point of using Lorem Ipsum is that it has a more-or-less normal distribution of letters, as opposed to using"
          />
        </Container>
      </FullScreenDialog>
    </>
  );
};

export default PipelineGraph;
