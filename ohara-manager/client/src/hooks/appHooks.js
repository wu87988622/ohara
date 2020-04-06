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

import { useCallback, useEffect } from 'react';
import { find, filter, head, isEmpty } from 'lodash';
import { useHistory, useParams } from 'react-router-dom';
import { useSelector } from 'react-redux';

import { hashByGroupAndName } from 'utils/sha';
import * as context from 'context';
import * as hooks from 'hooks';

export const useIsAppReady = () => {
  const mapState = useCallback(state => !!state.ui.app.lastUpdated, []);
  return useSelector(mapState);
};

export const useWelcome = () => {
  const isAppReady = useIsAppReady();
  const allWorkspaces = hooks.useAllWorkspaces();
  const wasIntroOpened = hooks.useWasIntroOpened();
  const openIntro = hooks.useOpenIntroAction();

  useEffect(() => {
    if (isAppReady && !wasIntroOpened && isEmpty(allWorkspaces)) openIntro();
  }, [isAppReady, allWorkspaces, wasIntroOpened, openIntro]);
};

export const useRedirect = () => {
  const isAppReady = useIsAppReady();
  const allWorkspaces = hooks.useAllWorkspaces();
  const allPipelines = hooks.useAllPipelines();
  const switchWorkspace = hooks.useSwitchWorkspaceAction();
  const switchPipeline = hooks.useSwitchPipelineAction();
  const workspaceName = hooks.useWorkspaceName();
  const pipelineName = hooks.usePipelineName();
  const { setWorkspaceName, setPipelineName } = context.useApp();

  const history = useHistory();
  const {
    workspaceName: routedWorkspaceName,
    pipelineName: routedPipelineName,
  } = useParams();

  useEffect(() => {
    if (!isAppReady) return;

    const validWorkspace =
      find(allWorkspaces, wk => wk.name === routedWorkspaceName) ||
      head(allWorkspaces);

    const validPipelines = filter(
      allPipelines,
      pl =>
        pl.group ===
        hashByGroupAndName(validWorkspace?.group, validWorkspace?.name),
    );
    const validPipeline =
      find(validPipelines, pl => pl.name === routedPipelineName) ||
      head(validPipelines);

    const validWorkspaceName = validWorkspace?.name;
    const validPipelineName = validPipeline?.name;

    if (
      routedWorkspaceName !== validWorkspaceName ||
      routedPipelineName !== validPipelineName
    ) {
      if (validWorkspaceName && validPipelineName) {
        history.push(`/${validWorkspaceName}/${validPipelineName}`);
      } else if (validWorkspaceName) {
        history.push(`/${validWorkspaceName}`);
      } else {
        history.push(`/`);
      }
    }
  }, [
    history,
    isAppReady,
    allPipelines,
    allWorkspaces,
    routedPipelineName,
    routedWorkspaceName,
  ]);

  useEffect(() => {
    if (routedWorkspaceName && routedWorkspaceName !== workspaceName) {
      setWorkspaceName(routedWorkspaceName);
      switchWorkspace(routedWorkspaceName);
    }
  }, [workspaceName, routedWorkspaceName, setWorkspaceName, switchWorkspace]);

  useEffect(() => {
    if (routedPipelineName && routedPipelineName !== pipelineName) {
      setPipelineName(routedPipelineName);
      switchPipeline(routedPipelineName);
    }
  }, [pipelineName, routedPipelineName, setPipelineName, switchPipeline]);
};
