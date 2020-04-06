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

import { useCallback, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { isEqual as isDeepEqual, some } from 'lodash';

import * as hooks from 'hooks';
import * as actions from 'store/actions';

export const useIsCreateWorkspaceDialogOpen = () =>
  useSelector(state => !!state.ui.createWorkspace.isOpen);

export const useCreateWorkspaceMode = () =>
  useSelector(state => state.ui.createWorkspace.mode);

export const useCreateWorkspaceStep = () =>
  useSelector(state => state.ui.createWorkspace.step);

export const useCreateWorkspaceProgress = () => {
  const mapState = useCallback(state => state.ui.createWorkspace.progress, []);
  return useSelector(mapState, isDeepEqual);
};

export const useCreateWorkspaceState = () => {
  const mapState = useCallback(state => state.ui.createWorkspace, []);
  return useSelector(mapState, isDeepEqual);
};

export const useUniqueWorkspaceName = (prefix = 'workspace') => {
  const workspaces = hooks.useWorkspaces();
  return useMemo(() => {
    for (var postfix = 1; postfix <= Number.MAX_SAFE_INTEGER; postfix++) {
      const name = `${prefix}${postfix}`;
      if (!some(workspaces, { name })) return name;
    }
  }, [prefix, workspaces]);
};

export const useOpenCreateWorkspaceDialogAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.openCreateWorkspace.trigger()), [
    dispatch,
  ]);
};

export const useCloseCreateWorkspaceDialogAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.closeCreateWorkspace.trigger()), [
    dispatch,
  ]);
};

export const useSwitchCreateWorkspaceModeAction = () => {
  const dispatch = useDispatch();
  return useCallback(
    mode => dispatch(actions.switchCreateWorkspaceMode.trigger(mode)),
    [dispatch],
  );
};

export const useSwitchCreateWorkspaceStepAction = () => {
  const dispatch = useDispatch();
  return useCallback(
    step => dispatch(actions.switchCreateWorkspaceStep.trigger(step)),
    [dispatch],
  );
};

export const useCreateWorkspaceAction = () => {
  const dispatch = useDispatch();
  return useCallback(
    values => dispatch(actions.createWorkspace.trigger(values)),
    [dispatch],
  );
};
