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

import { useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';

import { GROUP } from 'const';
import * as hooks from 'hooks';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { getId } from 'utils/object';

export const useWorkspaceGroup = () => GROUP.WORKSPACE;

export const useWorkspaceName = () =>
  useSelector(useCallback(state => selectors.getWorkspaceName(state), []));

export const useWorkspaceId = () => {
  const group = useWorkspaceGroup();
  const name = useWorkspaceName();
  return getId({ group, name });
};

export const useSwitchWorkspaceAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useWorkspaceGroup();
  return useCallback(
    name => dispatch(actions.switchWorkspace.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useAllWorkspaces = () =>
  useSelector(useCallback(state => selectors.getAllWorkspaces(state), []));

export const useWorkspace = () => {
  const id = hooks.useWorkspaceId();
  return useSelector(
    useCallback(state => selectors.getWorkspaceById(state, { id }), [id]),
  );
};
