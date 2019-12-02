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

import React, { useEffect, useState } from 'react';
import { useHistory, useParams } from 'react-router-dom';

import NodeDialog from 'components/Node/NodeDialog';
import IntroDialog from './IntroDialog';
import Graph from './Graph';
import { useWorkspace } from 'context';
import { usePipelineActions, usePipelineState } from 'context';
import { useNewWorkspace } from 'context/NewWorkspaceContext';
import { usePrevious } from 'utils/hooks';

const Pipeline = () => {
  const history = useHistory();
  const {
    workspaces,
    isFetching: isFetchingWorkspaces,
    currentWorkspace: currentWorker,
  } = useWorkspace();
  const { fetchPipelines } = usePipelineActions();
  const { data: pipelines } = usePipelineState();
  const { workspaceName, pipelineName } = useParams();
  const { setIsOpen: setIsNewWorkspaceDialogOpen } = useNewWorkspace();
  const [isToolboxOpen, setIsToolboxOpen] = useState(true);
  const [toolboxKey, setToolboxKey] = useState(0);

  const initialState = {
    topic: false,
    source: false,
    sink: false,
    stream: false,
  };

  const [toolboxExpanded, setToolboxExpanded] = useState(initialState);

  const handleToolboxClick = panel => {
    setToolboxExpanded(prevState => {
      return {
        ...prevState,
        [panel]: !prevState[panel],
      };
    });
  };

  const hasWorkspace = workspaces.length > 0;
  let currentWorkspace;

  if (workspaceName && isFetchingWorkspaces === false) {
    const current = workspaces.find(
      workspace => workspace.settings.name === workspaceName,
    );

    // If the `current` pipeline is found in the pipeline list
    if (current) currentWorkspace = current;

    // If the `current` workspace is not found in the list but the
    // list is not empty, let's display the first workspace
    if (current === undefined && hasWorkspace) {
      history.push(`/${workspaces[0].settings.name}`);
    }
  } else if (hasWorkspace) {
    history.push(`/${workspaces[0].settings.name}`);
  }

  useEffect(() => {
    if (hasWorkspace) {
      setIsNewWorkspaceDialogOpen(false);
    } else {
      setIsNewWorkspaceDialogOpen(true);
    }
  }, [hasWorkspace, setIsNewWorkspaceDialogOpen]);

  useEffect(() => {
    if (!currentWorker) return;
    fetchPipelines(currentWorker.settings.name);
  }, [fetchPipelines, currentWorker, workspaceName]);

  const hasPipeline = pipelines.length > 0;
  let currentPipeline;
  if (pipelineName) {
    const current = pipelines.find(pipeline => pipeline.name === pipelineName);

    // If the `current` pipeline is found in the pipeline list
    if (current) currentPipeline = current;

    // If the `current` pipeline is not found in the list but the
    // list is not empty, let's display the first pipeline
    if (current === undefined && !hasPipeline) {
      history.push(`/${workspaceName}`);
    } else if (current === undefined && hasPipeline) {
      history.push(`/${workspaceName}/${pipelines[0].name}`);
    }
  } else {
    if (hasWorkspace && hasPipeline) {
      history.push(`/${workspaceName}/${pipelines[0].name}`);
    }
  }

  const prevPipeline = usePrevious(currentPipeline);
  // Reset toolbox states
  useEffect(() => {
    if (currentPipeline !== prevPipeline) {
      setToolboxExpanded(initialState);
      // updating the key "re-renders" the whole toolbox
      // which effectively resets the toolbox position
      // as well. Note we also use this key to update
      // the Graph related components
      setToolboxKey(prevKey => prevKey + 1);
    }
  }, [currentPipeline, initialState, prevPipeline]);

  return (
    <>
      {currentWorkspace && (
        <>
          {currentPipeline && (
            <Graph
              isToolboxOpen={isToolboxOpen}
              toolboxExpanded={toolboxExpanded}
              handleToolbarClick={panel =>
                // Open a particular panel
                setToolboxExpanded({ ...initialState, [panel]: true })
              }
              handleToolboxOpen={() => setIsToolboxOpen(true)}
              handleToolboxClick={handleToolboxClick}
              handleToolboxClose={() => {
                setIsToolboxOpen(false);
                setToolboxExpanded(initialState);
              }}
              toolboxKey={toolboxKey}
              setToolboxExpanded={setToolboxExpanded}
            />
          )}
        </>
      )}

      {!isFetchingWorkspaces && <IntroDialog />}
      <NodeDialog />
    </>
  );
};

export default Pipeline;
