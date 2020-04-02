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
import { get, merge, reject, filter } from 'lodash';

import * as hooks from 'hooks';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { getId } from 'utils/object';
import { hashByGroupAndName } from 'utils/sha';
import { KIND } from 'const';

export const useIsTopicLoaded = () => {
  const mapState = useCallback(state => !!state.ui.topic?.lastUpdated, []);
  return useSelector(mapState);
};

export const useIsTopicLoading = () => {
  const mapState = useCallback(state => !!state.ui.topic?.loading, []);
  return useSelector(mapState);
};

export const useTopicGroup = () => {
  const workspaceGroup = hooks.useWorkspaceGroup();
  const workspaceName = hooks.useWorkspaceName();
  if (workspaceGroup && workspaceName)
    return hashByGroupAndName(workspaceGroup, workspaceName);
};

export const useFetchAllTopicsAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.fetchTopics.trigger()), [dispatch]);
};

export const useCreateTopicAction = () => {
  const dispatch = useDispatch();
  const group = useTopicGroup();
  const brokerClusterKey = {
    group: hooks.useBrokerGroup(),
    name: hooks.useWorkspaceName(),
  };
  return useCallback(
    values =>
      dispatch(
        actions.createTopic.request({ ...values, group, brokerClusterKey }),
      ),
    [brokerClusterKey, dispatch, group],
  );
};

export const useUpdateTopicAction = () => {
  const dispatch = useDispatch();
  const group = useTopicGroup();
  return useCallback(
    values => dispatch(actions.updateTopic.request({ ...values, group })),
    [dispatch, group],
  );
};

export const useDeleteTopicAction = () => {
  const dispatch = useDispatch();
  const group = useTopicGroup();
  return useCallback(
    name => dispatch(actions.deleteTopic.request({ group, name })),
    [dispatch, group],
  );
};

export const useStartTopicAction = () => {
  const dispatch = useDispatch();
  const group = useTopicGroup();
  return useCallback(
    name => dispatch(actions.startTopic.request({ group, name })),
    [dispatch, group],
  );
};

export const useStopTopicAction = () => {
  const dispatch = useDispatch();
  const group = useTopicGroup();
  return useCallback(
    name => dispatch(actions.stopTopic.request({ group, name })),
    [dispatch, group],
  );
};

export const useCreateAndStartTopicAction = () => {
  const dispatch = useDispatch();
  const group = useTopicGroup();
  const brokerClusterKey = {
    group: hooks.useBrokerGroup(),
    name: hooks.useWorkspaceName(),
  };
  return useCallback(
    values =>
      dispatch(
        actions.createAndStartTopic.request({
          ...values,
          group,
          brokerClusterKey,
        }),
      ),
    [brokerClusterKey, dispatch, group],
  );
};

export const useStopAndDeleteTopicAction = () => {
  const dispatch = useDispatch();
  const group = useTopicGroup();
  return useCallback(
    values =>
      dispatch(actions.stopAndDeleteTopic.request({ ...values, group })),
    [dispatch, group],
  );
};

export const useAllTopics = () => {
  const getAllTopics = useMemo(selectors.makeGetAllTopics, []);
  return useSelector(useCallback(state => getAllTopics(state), [getAllTopics]));
};

export const useTopicsInWorkspace = () => {
  const group = useTopicGroup();
  const getTopicsByGroup = useMemo(selectors.makeGetTopicsByGroup, []);
  return useSelector(
    useCallback(state => getTopicsByGroup(state, { group }), [
      getTopicsByGroup,
      group,
    ]),
  );
};

/**
 * Filter the topics which are displayed in current toolbox of pipeline
 *
 * Note: We will filter out
 * 1. Pipeline-only topics
 * 2. "Shared" topics that are not running
 */
export const useTopicsInToolbox = () => {
  const group = useTopicGroup();
  const getTopicsByGroup = useMemo(selectors.makeGetTopicsByGroup, []);
  return useSelector(
    useCallback(
      state =>
        reject(
          getTopicsByGroup(state, { group }),
          topic => !topic.tags.isShared || topic.state !== 'RUNNING',
        ),
      [getTopicsByGroup, group],
    ),
  );
};

/**
 * Filter the topics which are displayed in current pipeline
 *
 * Note: We will only show topics which are added in paper
 */
export const useTopicsInPipeline = () => {
  const currentPipeline = hooks.usePipeline();
  const group = useTopicGroup();
  const getTopicsByGroup = useMemo(selectors.makeGetTopicsByGroup, []);
  return useSelector(
    useCallback(
      state => {
        const topicKeys = filter(
          get(currentPipeline, 'endpoints', []),
          endpoint => endpoint.kind === KIND.topic,
        ).map(endpoint => ({ name: endpoint.name, group: endpoint.group }));
        return filter(getTopicsByGroup(state, { group }), topic =>
          topicKeys.includes({ name: topic.name, group: topic.group }),
        ).sort((current, next) => {
          const currentName = current.tags.isShared
            ? current.name
            : current.tags.displayName;
          const nextName = next.tags.isShared
            ? next.name
            : next.tags.displayName;
          return currentName.localeCompare(nextName);
        });
      },
      [currentPipeline, getTopicsByGroup, group],
    ),
  );
};

export const useTopic = name => {
  const getTopicById = useMemo(selectors.makeGetTopicById, []);
  const brokerData = hooks.useBroker();
  // broker only have "one" classInfo (i.e., topic definition)
  const info = get(brokerData, 'classInfos[0]', {});
  const group = useTopicGroup();
  const id = getId({ group, name });
  return useSelector(
    useCallback(
      state => {
        const topic = getTopicById(state, { id });
        return merge(topic, info);
      },
      [getTopicById, id, info],
    ),
  );
};
