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

import { useApp } from 'context';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { getId } from 'utils/object';

export const useCurrentPipelineId = () => {
  const { pipelineGroup: group, pipelineName: name } = useApp();
  return getId({ group, name });
};

export const useAllPipelines = () => {
  const getAllPipelines = useMemo(selectors.makeGetAllPipelines, []);

  const pipelines = useMappedState(
    useCallback(state => getAllPipelines(state), [getAllPipelines]),
  );
  return pipelines;
};

export const useCurrentPipelines = () => {
  const makeFindPipelinesByGroup = useMemo(
    selectors.makeFindPipelinesByGroup,
    [],
  );
  const { pipelineGroup: group } = useApp();
  const findPipelinesByGroup = useCallback(
    state => makeFindPipelinesByGroup(state, { group }),
    [makeFindPipelinesByGroup, group],
  );
  return useMappedState(findPipelinesByGroup);
};

export const useCurrentPipeline = () => {
  const id = useCurrentPipelineId();
  const getPipelineById = useMemo(selectors.makeGetPipelineById, []);

  const pipeline = useMappedState(
    useCallback(state => getPipelineById(state, { id }), [getPipelineById, id]),
  );
  return pipeline;
};

export const useCurrentPipelineCell = () =>
  useMappedState(useCallback(state => state?.ui?.pipeline?.selectedCell, []));

export const useSetSelectedCellAction = () => {
  const dispatch = useDispatch();
  return function(cell) {
    dispatch(actions.setSelectedCell.trigger(cell));
  };
};

export const useFetchPipelineAction = () => {
  const dispatch = useDispatch();
  const { pipelineGroup } = useApp();
  return function(name) {
    const params = { name, group: pipelineGroup };
    dispatch(actions.fetchPipeline.trigger(params));
  };
};

export const useCreatePipelineAction = () => {
  const dispatch = useDispatch();
  const { pipelineGroup } = useApp();
  return function(values) {
    const finalValues = { ...values, group: pipelineGroup };
    dispatch(actions.createPipeline.trigger(finalValues));
  };
};

export const useUpdatePipelineAction = () => {
  const dispatch = useDispatch();
  const { pipelineGroup } = useApp();
  return function(values) {
    const finalValues = { ...values, group: pipelineGroup };
    dispatch(actions.updatePipeline.trigger(finalValues));
  };
};

export const useDeletePipelineAction = () => {
  const dispatch = useDispatch();
  const { pipelineGroup } = useApp();
  return function(name) {
    const params = { name, group: pipelineGroup };
    dispatch(actions.deletePipeline.trigger(params));
  };
};
