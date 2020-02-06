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

import { useReducer, useEffect } from 'react';
import { useHistory } from 'react-router-dom';

import * as context from 'context';
import { useLocalStorage } from 'utils/hooks';

export const usePipelineState = () => {
  const [isMetricsOn, setIsMetricsOn] = useLocalStorage(
    'isPipelineMetricsOn',
    null,
  );

  const initialState = {
    isToolboxOpen: true,
    toolboxKey: 0,
    isMetricsOn,
    toolboxExpanded: {
      topic: false,
      source: false,
      sink: false,
      stream: false,
    },
  };

  const reducer = (state, action) => {
    const { type, payload } = action;
    switch (type) {
      case 'openToolbox':
        return {
          ...state,
          isToolboxOpen: true,
        };

      case 'closeToolbox':
        return {
          ...state,
          isToolboxOpen: false,
          toolboxExpanded: initialState.toolboxExpanded,
        };

      case 'setToolbox':
        return {
          ...state,
          toolboxExpanded: {
            ...state.toolboxExpanded,
            [payload]: !state.toolboxExpanded[payload],
          },
        };

      case 'setMultiplePanels':
        return {
          ...state,
          toolboxExpanded: {
            ...state.toolboxExpanded,
            ...payload,
          },
        };

      case 'setToolboxKey':
        return {
          ...state,
          toolboxKey: state.toolboxKey + 1,
        };

      case 'resetToolbox':
        return {
          ...state,
          toolboxExpanded: initialState.toolboxExpanded,
        };

      case 'toggleMetricsButton':
        setIsMetricsOn(!state.isMetricsOn);
        return {
          ...state,
          isMetricsOn: !state.isMetricsOn,
        };

      default:
        return state;
    }
  };

  return useReducer(reducer, initialState);
};

export const useRedirect = () => {
  const { workspaceName, pipelineName } = context.useApp();
  const history = useHistory();

  const { workspaces } = context.useWorkspace();
  const { lastUpdated: isWorkspaceReady } = context.useWorkspaceState();
  const {
    data: pipelines,
    lastUpdated: isPipelineReady,
  } = context.usePipelineState();

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

    // pipelines exist in current workspace and pipeline name in path
    if (pipelineName && isPipelineReady) {
      // the pipeline name in path not exist
      if (!hasCurrentPipeline) {
        // redirect to default workspace and pipeline
        if (!hasCurrentWorkspace) {
          const url = hasPipeline
            ? `/${workspaces[0].name}/${pipelines[0].name}`
            : `/${workspaces[0].name}`;
          history.push(url);
          // redirect to current workspace and default pipeline
        } else {
          const url = hasPipeline
            ? `/${workspaceName}/${pipelines[0].name}`
            : `/${workspaceName}`;
          history.push(url);
        }
      } else {
        history.push(`/${workspaceName}/${pipelineName}`);
      }
      // pipelines exist in current workspace but pipeline name not in path
    } else if (isPipelineReady && hasWorkspace && hasPipeline) {
      history.push(`/${workspaceName}/${pipelines[0].name}`);
      // only workspace in the path
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
};
