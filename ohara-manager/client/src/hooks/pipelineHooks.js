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

import * as hooks from 'hooks';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { getId } from 'utils/object';
import { hashByGroupAndName } from 'utils/sha';

export const usePipelineGroup = () => {
  const workspaceGroup = hooks.useWorkspaceGroup();
  const workspaceName = hooks.useWorkspaceName();
  if (workspaceGroup && workspaceName)
    return hashByGroupAndName(workspaceGroup, workspaceName);
};

export const usePipelineName = () => {
  const mapState = useCallback(state => state.ui.pipeline.name, []);
  return useSelector(mapState);
};

export const useSwitchPipelineAction = () => {
  const dispatch = useDispatch();
  const group = hooks.usePipelineGroup();
  return useCallback(
    name => dispatch(actions.switchPipeline.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useFetchPipelineAction = () => {
  const dispatch = useDispatch();
  const group = hooks.usePipelineGroup();
  return useCallback(
    name => dispatch(actions.fetchPipeline.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useCreatePipelineAction = () => {
  const dispatch = useDispatch();
  const group = hooks.usePipelineGroup();
  return useCallback(
    values => dispatch(actions.createPipeline.trigger({ ...values, group })),
    [dispatch, group],
  );
};

export const useUpdatePipelineAction = () => {
  const dispatch = useDispatch();
  const group = hooks.usePipelineGroup();
  return useCallback(
    values => dispatch(actions.updatePipeline.trigger({ ...values, group })),
    [dispatch, group],
  );
};

export const useDeletePipelineAction = () => {
  const dispatch = useDispatch();
  const group = hooks.usePipelineGroup();
  return useCallback(
    name => dispatch(actions.deletePipeline.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useSetSelectedCellAction = () => {
  const dispatch = useDispatch();
  return useCallback(cell => dispatch(actions.setSelectedCell.trigger(cell)), [
    dispatch,
  ]);
};

export const useAllPipelines = () => {
  const getAllPipelines = useMemo(selectors.makeGetAllPipelines, []);
  return useSelector(
    useCallback(state => getAllPipelines(state), [getAllPipelines]),
  );
};

export const usePipelines = () => {
  const makeFindPipelinesByGroup = useMemo(
    selectors.makeFindPipelinesByGroup,
    [],
  );
  const group = hooks.usePipelineGroup();
  const findPipelinesByGroup = useCallback(
    state => makeFindPipelinesByGroup(state, { group }),
    [makeFindPipelinesByGroup, group],
  );
  return useSelector(findPipelinesByGroup);
};

export const usePipeline = () => {
  const getPipelineById = useMemo(selectors.makeGetPipelineById, []);
  const group = usePipelineGroup();
  const name = usePipelineName();
  const id = getId({ group, name });
  return useSelector(
    useCallback(state => getPipelineById(state, { id }), [getPipelineById, id]),
  );
};

export const useCurrentPipelineCell = () => {
  const mapState = useCallback(state => state.ui.pipeline.selectedCell, []);
  return useSelector(mapState);
};
