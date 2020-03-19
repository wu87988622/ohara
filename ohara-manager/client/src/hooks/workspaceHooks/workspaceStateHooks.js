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
import { useMappedState } from 'redux-react-hook';
import { isEqual as isDeepEqual } from 'lodash';

import { useApp } from 'context';
import * as selectors from 'store/selectors';
import { getId } from 'utils/object';

export const useCurrentWorkspaceId = () => {
  const { workspaceGroup: group, workspaceName: name } = useApp();
  return getId({ group, name });
};

export const useAllWorkspaces = () => {
  const getAllWorkspaces = useMemo(selectors.makeGetAllWorkspaces, []);
  const workspaces = useMappedState(
    useCallback(state => getAllWorkspaces(state), [getAllWorkspaces]),
  );
  return workspaces;
};

export const useCurrentWorkspace = () => {
  const id = useCurrentWorkspaceId();
  const getWorkspaceById = useMemo(selectors.makeGetWorkspaceById, []);

  const workspace = useMappedState(
    useCallback(state => getWorkspaceById(state, { id }), [
      getWorkspaceById,
      id,
    ]),
  );
  return workspace;
};

export const useCreateWorkspaceProgress = () => {
  const mapState = useCallback(state => state.ui.createWorkspace.progress, []);
  return useMappedState(mapState, isDeepEqual);
};

export const useCreateWorkspaceState = () => {
  const mapState = useCallback(state => state.ui.createWorkspace, []);
  return useMappedState(mapState, isDeepEqual);
};
