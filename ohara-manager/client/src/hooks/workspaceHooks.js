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
import { useDispatch, useMappedState } from 'redux-react-hook';

import * as hooks from 'hooks';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { getId } from 'utils/object';

export const useWorkspaceGroup = () => 'workspace';

export const useWorkspaceName = () => {
  const mapState = useCallback(state => state.ui.workspace.name, []);
  return useMappedState(mapState);
};

export const useSwitchWorkspaceAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useWorkspaceGroup();
  return useCallback(
    name => dispatch(actions.switchWorkspace.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useAllWorkspaces = () => {
  const getAllWorkspaces = useMemo(selectors.makeGetAllWorkspaces, []);
  return useMappedState(
    useCallback(state => getAllWorkspaces(state), [getAllWorkspaces]),
  );
};

export const useWorkspaces = () => {
  const workspaces = useAllWorkspaces();
  return workspaces;
};

export const useWorkspace = () => {
  const getWorkspaceById = useMemo(selectors.makeGetWorkspaceById, []);
  const group = useWorkspaceGroup();
  const name = useWorkspaceName();
  const id = getId({ group, name });
  return useMappedState(
    useCallback(state => getWorkspaceById(state, { id }), [
      getWorkspaceById,
      id,
    ]),
  );
};
