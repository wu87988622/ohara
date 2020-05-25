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

import { useCallback, useMemo, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { get, merge, reject } from 'lodash';

import * as hooks from 'hooks';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { getId, isEqualByKey } from 'utils/object';
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

export const useFetchTopicsAction = () => {
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
        actions.createTopic.trigger({ ...values, group, brokerClusterKey }),
      ),
    [brokerClusterKey, dispatch, group],
  );
};

export const useUpdateTopicAction = () => {
  const dispatch = useDispatch();
  const group = useTopicGroup();
  return useCallback(
    values => dispatch(actions.updateTopic.trigger({ ...values, group })),
    [dispatch, group],
  );
};

export const useDeleteTopicAction = () => {
  const dispatch = useDispatch();
  const group = useTopicGroup();
  return useCallback(
    name => dispatch(actions.deleteTopic.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useStartTopicAction = () => {
  const dispatch = useDispatch();
  const group = useTopicGroup();
  return useCallback(
    name => dispatch(actions.startTopic.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useStopTopicAction = () => {
  const dispatch = useDispatch();
  const group = useTopicGroup();
  return useCallback(
    name => dispatch(actions.stopTopic.trigger({ group, name })),
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
    (values, options) => {
      return new Promise((resolve, reject) => {
        dispatch(
          actions.createAndStartTopic.trigger({
            params: { ...values, group, brokerClusterKey },
            options,
            promise: { resolve, reject },
          }),
        );
      });
    },
    [brokerClusterKey, dispatch, group],
  );
};

export const useStopAndDeleteTopicAction = () => {
  const dispatch = useDispatch();
  const group = useTopicGroup();
  return useCallback(
    (values, options) => {
      return new Promise((resolve, reject) => {
        dispatch(
          actions.stopAndDeleteTopic.trigger({
            params: { ...values, group },
            options,
            promise: { resolve, reject },
          }),
        );
      });
    },
    [dispatch, group],
  );
};

export const useAllTopics = () => {
  const isTopicLoaded = hooks.useIsTopicLoaded();
  const fetchTopics = hooks.useFetchTopicsAction();
  const brokerId = hooks.useBrokerId();

  useEffect(() => {
    if (!isTopicLoaded) fetchTopics();
  }, [fetchTopics, isTopicLoaded]);

  return useSelector(state => {
    const topics = selectors.getAllTopics(state);
    const results = topics.map(topic => {
      const info = selectors.getInfoById(state, { id: brokerId });

      const settingDefinitions =
        info?.classInfos.find(def => def.classType === KIND.topic)
          ?.settingDefinitions || [];

      return merge(topic, { settingDefinitions });
    });
    return results;
  });
};

export const useTopicsInWorkspace = isShared => {
  const getTopicsByGroup = useMemo(() => {
    if (isShared === true) {
      return selectors.getSharedTopicsByGroup;
    } else if (isShared === false) {
      return selectors.getPipelineOnlyTopicsByGroup;
    } else {
      return selectors.getTopicsByGroup;
    }
  }, [isShared]);
  const group = useTopicGroup();
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
  return useSelector(
    useCallback(
      state =>
        reject(
          selectors.getTopicsByGroup(state, { group }),
          topic => !topic.isShared || topic.state !== 'RUNNING',
        ),
      [group],
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
  const endpoints = get(currentPipeline, 'endpoints', []);
  const brokerData = hooks.useBroker();
  // broker only has "one" classInfo (i.e., topic definition)
  const info = get(brokerData, 'classInfos[0]', {});

  return useSelector(
    useCallback(
      state => {
        const topicEndpoints = endpoints.filter(
          endpoint => endpoint.kind === KIND.topic,
        );
        const topics = selectors.getTopicsByGroup(state, { group });

        const getTopicByKey = topic =>
          topicEndpoints.some(endpoint => isEqualByKey(topic, endpoint));

        return topics
          .filter(getTopicByKey)
          .map(topic => merge(topic, info))
          .sort((current, next) => {
            const currentName = current.displayName;
            const nextName = next.displayName;
            return currentName.localeCompare(nextName);
          });
      },
      [endpoints, group, info],
    ),
  );
};

export const useTopic = name => {
  const brokerData = hooks.useBroker();
  // broker only has "one" classInfo (i.e., topic definition)
  const info = get(brokerData, 'classInfos[0]', {});
  const group = useTopicGroup();
  const id = getId({ group, name });
  return useSelector(
    useCallback(
      state => {
        const topic = selectors.getTopicById(state, { id });
        return merge(topic, info);
      },
      [id, info],
    ),
  );
};
