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

import * as hooks from 'hooks';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { getId } from 'utils/object';
import { hashByGroupAndName } from 'utils/sha';
import { KIND } from 'const';

export const usePipelineGroup = () => {
  const workspaceGroup = hooks.useWorkspaceGroup();
  const workspaceName = hooks.useWorkspaceName();
  if (workspaceGroup && workspaceName)
    return hashByGroupAndName(workspaceGroup, workspaceName);
};

export const usePipelineName = () => {
  const selector = useCallback(state => state.ui.pipeline.name, []);
  return useSelector(selector);
};

export const useIsPipelineDeleting = () => {
  const selector = useCallback(state => state.ui.pipeline.deleting, []);
  return useSelector(selector);
};

export const usePipelineError = () => {
  const selector = useCallback(state => state.ui.pipeline.error, []);
  return useSelector(selector);
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

const updateEndpointGroup = ({
  values,
  streamAndConnectorGroup,
  topicGroup,
}) => {
  return values.endpoints.map(endpoint => {
    const { kind } = endpoint;
    if (kind === KIND.source || kind === KIND.sink || kind === KIND.stream) {
      return { ...endpoint, group: streamAndConnectorGroup };
    } else if (kind === KIND.topic) {
      return { ...endpoint, group: topicGroup };
    }

    return endpoint;
  });
};

export const useUpdatePipelineAction = () => {
  const dispatch = useDispatch();
  const group = hooks.usePipelineGroup();

  // Both connector and stream are using same group key
  const streamAndConnectorGroup = hooks.useStreamGroup();
  const topicGroup = hooks.useTopicGroup();

  return useCallback(
    values => {
      const endpoints = updateEndpointGroup({
        values,
        streamAndConnectorGroup,
        topicGroup,
      });

      dispatch(
        actions.updatePipeline.trigger({
          ...values,
          group,
          endpoints,
        }),
      );
    },
    [dispatch, group, streamAndConnectorGroup, topicGroup],
  );
};

export const useDeletePipelineAction = () => {
  const dispatch = useDispatch();
  const group = hooks.usePipelineGroup();
  return useCallback(
    (params, options) => {
      const newParams = { ...params, group };
      dispatch(actions.deletePipeline.trigger({ params: newParams, options }));
    },
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
  const selector = useCallback(state => selectors.getAllPipelines(state), []);
  return useSelector(selector);
};

export const usePipelines = () => {
  const group = hooks.usePipelineGroup();
  const selector = useCallback(
    state => selectors.findPipelinesByGroup(state, { group }),
    [group],
  );
  return useSelector(selector);
};

export const usePipeline = () => {
  const group = usePipelineGroup();
  const name = usePipelineName();
  const pipelineId = getId({ group, name });
  const selector = useCallback(
    state => selectors.getPipelineById(state, { id: pipelineId }),
    [pipelineId],
  );
  return useSelector(selector);
};

export const useCurrentPipelineCell = () => {
  const selector = useCallback(state => state.ui.pipeline.selectedCell, []);
  return useSelector(selector);
};
