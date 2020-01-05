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
import { useHistory } from 'react-router-dom';
import _ from 'lodash';

import * as context from 'context';
import NodeDialog from 'components/Node/NodeDialog';
import IntroDialog from './IntroDialog';
import SettingDialog from './SettingDialog';
import Graph from './Graph';
import { useNewWorkspace } from 'context/NewWorkspaceContext';
import { useApp, useConnectorActions, useTopicActions } from 'context';
import { usePrevious } from 'utils/hooks';

const Pipeline = () => {
  const history = useHistory();
  const { workspaceName, pipelineName } = useApp();
  const {
    workspaces,
    currentWorkspace,
    currentPipeline,
  } = context.useWorkspace();
  const { lastUpdated: isWorkspaceReady } = context.useWorkspaceState();
  const { fetchPipelines } = context.usePipelineActions();
  const {
    data: pipelines,
    lastUpdated: isPipelineReady,
  } = context.usePipelineState();
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

  useEffect(() => {
    if (!isWorkspaceReady) return;

    const hasWorkspace = workspaces.length > 0;
    const hasPipeline = pipelines.length > 0;
    const hasCurrentWorkspace = workspaces.some(
      workspace => workspace.name === workspaceName,
    );
    const hasCurrentPipeline = pipelines.some(
      pipeline => pipeline.name === pipelineName,
    );

    if (pipelineName && isPipelineReady) {
      if (!hasCurrentPipeline) {
        const url = hasPipeline
          ? `/${workspaceName}/${pipelines[0].name}`
          : `/${workspaceName}`;
        history.push(url);
      } else {
        history.push(`/${workspaceName}/${pipelineName}`);
      }
    } else if (isPipelineReady && hasWorkspace && hasPipeline) {
      history.push(`/${workspaceName}/${pipelines[0].name}`);
    } else if (workspaceName) {
      if (!hasCurrentWorkspace) {
        const url = hasWorkspace ? `/${workspaces[0].name}` : '/';
        history.push(url);
      } else {
        history.push(`/${workspaceName}`);
      }
    } else if (hasWorkspace) {
      history.push(`/${workspaces[0].name}`);
    }
  }, [
    history,
    isPipelineReady,
    isWorkspaceReady,
    pipelineName,
    pipelines,
    workspaceName,
    workspaces,
  ]);

  useEffect(() => {
    if (!isWorkspaceReady) return;
    if (workspaces.length > 0) {
      return setIsNewWorkspaceDialogOpen(false);
    }

    setIsNewWorkspaceDialogOpen(true);
  }, [isWorkspaceReady, setIsNewWorkspaceDialogOpen, workspaces.length]);

  useEffect(() => {
    if (!currentWorkspace) return;
    fetchPipelines(currentWorkspace);
  }, [currentWorkspace, fetchPipelines]);

  const prevPipeline = usePrevious(currentPipeline);
  // Reset toolbox states
  useEffect(() => {
    const currentPipelineName = _.get(currentPipeline, 'name', '');
    const prevPipelineName = _.get(prevPipeline, 'name', '');
    if (currentPipelineName === prevPipelineName) return;

    setToolboxExpanded(initialState);
    // re-renders Toolbox
    setToolboxKey(prevKey => prevKey + 1);
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
              useConnectorActions={useConnectorActions}
              useTopicActions={useTopicActions}
            />
          )}
        </>
      )}

      <IntroDialog
        easyModeText={workspaces.length > 0 ? 'QUICK CREATE' : 'QUICK START'}
      />
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
