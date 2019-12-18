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

import * as context from 'context';
import NodeDialog from 'components/Node/NodeDialog';
import IntroDialog from './IntroDialog';
import SettingDialog from './SettingDialog';
import Graph from './Graph';
import { useNewWorkspace } from 'context/NewWorkspaceContext';
import { usePrevious } from 'utils/hooks';

const Pipeline = () => {
  const history = useHistory();
  const { workspaces, currentWorkspace } = context.useWorkspace();
  const { lastUpdated: workspaceLastUpdated } = context.useWorkspaceState();
  const { fetchPipelines } = context.usePipelineActions();
  const {
    data: pipelines,
    currentPipeline,
    lastUpdated: pipelineLastUpdated,
  } = context.usePipelineState();
  const { workspaceName, pipelineName } = useParams();
  const { setIsOpen: setIsNewWorkspaceDialogOpen } = useNewWorkspace();
  const [isToolboxOpen, setIsToolboxOpen] = useState(true);
  const [toolboxKey, setToolboxKey] = useState(0);
  const {
    isOpen: openSettingDialog,
    close: closeSettingDialog,
    data: settingDialogData,
  } = context.useGraphSettingDialog();

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

  // Check if the given workspace name is valid
  if (workspaceName) {
    if (!currentWorkspace) {
      const url = hasWorkspace ? `/${workspaces[0].settings.name}` : '/';
      history.push(url);
    }
  } else if (hasWorkspace) {
    // Load a default workspace if there's one
    history.push(`/${workspaces[0].settings.name}`);
  }

  useEffect(() => {
    if (!workspaceLastUpdated) return;

    if (hasWorkspace) {
      return setIsNewWorkspaceDialogOpen(false);
    }

    setIsNewWorkspaceDialogOpen(true);
  }, [hasWorkspace, setIsNewWorkspaceDialogOpen, workspaceLastUpdated]);

  useEffect(() => {
    if (!currentWorkspace) return;
    fetchPipelines(currentWorkspace.settings.name);
  }, [currentWorkspace, fetchPipelines]);

  const hasPipeline = pipelines.length > 0;
  if (pipelineName && pipelineLastUpdated) {
    // No pipeline found, redirect back to workspace

    if (!currentPipeline) {
      const url = hasPipeline
        ? `/${workspaceName}/${pipelines[0].name}`
        : `/${workspaceName}`;
      history.push(url);
    }
  } else if (
    !currentPipeline &&
    hasWorkspace &&
    hasPipeline &&
    pipelineLastUpdated
  ) {
    history.push(`/${workspaceName}/${pipelines[0].name}`);
  }

  const prevPipeline = usePrevious(currentPipeline);
  // Reset toolbox states
  useEffect(() => {
    if (currentPipeline !== prevPipeline) {
      setToolboxExpanded(initialState);
      // renders Toolbox
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

      <IntroDialog />
      <NodeDialog />
      <SettingDialog
        open={openSettingDialog}
        handleClose={closeSettingDialog}
        data={settingDialogData}
      />
    </>
  );
};

export default Pipeline;
