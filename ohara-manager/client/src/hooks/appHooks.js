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

import { useContext, useEffect } from 'react';
import { find, filter, head } from 'lodash';
import { useHistory, useParams } from 'react-router-dom';
import { StoreContext } from 'redux-react-hook';

import { hashByGroupAndName } from 'utils/sha';
import * as context from 'context';
import * as hooks from 'hooks';
import * as pipelineHooks from './pipelineHooks';
import * as workspaceHooks from './workspaceHooks';
import { usePrevious } from 'utils/hooks';

export const useIsAppReady = () => {
  const store = useContext(StoreContext);
  return !!store?.getState()?.ui?.app?.lastUpdated;
};

export const useRedirect = () => {
  const isAppReady = useIsAppReady();
  const workspaces = workspaceHooks.useAllWorkspaces();
  const pipelines = pipelineHooks.useAllPipelines();
  const { setWorkspaceName, setPipelineName } = context.useApp();

  const history = useHistory();
  const { workspaceName, pipelineName } = useParams();
  const prevWorkspaceName = usePrevious(workspaceName);
  const prevPipelineName = usePrevious(pipelineName);
  const switchWorkspace = hooks.useSwitchWorkspace();

  useEffect(() => {
    if (!isAppReady) return;

    const validWorkspace =
      find(workspaces, wk => wk.name === workspaceName) || head(workspaces);

    const validPipelines = filter(
      pipelines,
      pl =>
        pl.group ===
        hashByGroupAndName(validWorkspace?.group, validWorkspace?.name),
    );
    const validPipeline =
      find(validPipelines, pl => pl.name === pipelineName) ||
      head(validPipelines);

    const validWorkspaceName = validWorkspace?.name;
    const validPipelineName = validPipeline?.name;

    if (
      workspaceName !== validWorkspaceName ||
      pipelineName !== validPipelineName
    ) {
      if (validWorkspaceName && validPipelineName) {
        history.push(`/${validWorkspaceName}/${validPipelineName}`);
      } else if (validWorkspaceName) {
        history.push(`/${validWorkspaceName}`);
      } else {
        history.push(`/`);
      }
    }

    if (workspaceName === validWorkspaceName) switchWorkspace(workspaceName);
  }, [
    history,
    isAppReady,
    pipelineName,
    pipelines,
    switchWorkspace,
    workspaceName,
    workspaces,
  ]);

  useEffect(() => {
    if (workspaceName && workspaceName !== prevWorkspaceName) {
      setWorkspaceName(workspaceName);
    }
  }, [isAppReady, prevWorkspaceName, setWorkspaceName, workspaceName]);

  useEffect(() => {
    if (pipelineName && pipelineName !== prevPipelineName) {
      setPipelineName(pipelineName);
    }
  }, [pipelineName, prevPipelineName, setPipelineName]);
};
