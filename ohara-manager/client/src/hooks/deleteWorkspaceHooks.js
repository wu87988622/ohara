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

import * as actions from 'store/actions';

export const useOpenDeleteWorkspaceDialogAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.openDeleteWorkspace.trigger()), [
    dispatch,
  ]);
};

export const useDeleteWorkspace = () =>
  useSelector(state => state.ui.deleteWorkspace);

export const usePauseDeleteWorkspaceAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.pauseDeleteWorkspace.trigger()), [
    dispatch,
  ]);
};

export const useResumeDeleteWorkspaceAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.resumeDeleteWorkspace.trigger()), [
    dispatch,
  ]);
};

export const useRollbackDeleteWorkspaceAction = () => {
  const dispatch = useDispatch();
  return useCallback(
    () => dispatch(actions.rollbackDeleteWorkspace.trigger()),
    [dispatch],
  );
};

export const useDeleteWorkspaceAction = () => {
  const dispatch = useDispatch();
  return useCallback(
    values => dispatch(actions.deleteWorkspace.trigger(values)),
    [dispatch],
  );
};
