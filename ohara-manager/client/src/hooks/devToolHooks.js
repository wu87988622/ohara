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

import { get } from 'lodash';
import { useCallback, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';

import * as hooks from 'hooks';
import * as selectors from 'store/selectors';
import * as actions from 'store/actions';

// action hooks
// topic tab
export const useIsDevToolTopicDataQueried = () => {
  const topicData = hooks.useDevToolTopicData();
  return !!get(topicData, 'lastUpdated');
};

export const useFetchDevToolTopicDataAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useTopicGroup();
  const topics = hooks.useTopicsInWorkspace();
  return useCallback(
    () => dispatch(actions.fetchDevToolTopicData.trigger({ group, topics })),
    [dispatch, group, topics],
  );
};
export const useRefetchDevToolTopicDataAction = () => {
  const fetchTopicData = useFetchDevToolTopicDataAction();
  return () => fetchTopicData();
};
export const useSetDevToolTopicQueryParams = () => {
  const dispatch = useDispatch();
  const topicGroup = hooks.useTopicGroup();
  // we need to pass topics here since fetchTopicData should be triggered after setting
  const topics = hooks.useTopicsInWorkspace();
  return useCallback(
    params =>
      dispatch(
        actions.setDevToolTopicQueryParams.trigger({
          params,
          topicGroup,
          topics,
        }),
      ),
    [dispatch, topicGroup, topics],
  );
};

// log tab
export const useIsDevToolLogQueried = () => {
  const log = hooks.useDevToolLog();
  return !!get(log, 'lastUpdated');
};

export const useFetchDevToolLog = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.fetchDevToolLog.trigger()), [
    dispatch,
  ]);
};
export const useRefetchDevToolLog = () => {
  const fetchLog = useFetchDevToolLog();
  return () => fetchLog();
};
export const useSetDevToolLogQueryParams = () => {
  const dispatch = useDispatch();
  const shabondiGroup = hooks.useShabondiGroup();
  const streamGroup = hooks.useStreamGroup();
  return useCallback(
    params =>
      dispatch(
        actions.setDevToolLogQueryParams.trigger({
          params,
          shabondiGroup,
          streamGroup,
        }),
      ),
    [dispatch, shabondiGroup, streamGroup],
  );
};

// data selector
//topic tab
export const useDevToolTopicData = () => {
  const getTopicData = useMemo(selectors.makeGetDevToolTopicData, []);
  return useSelector(useCallback(state => getTopicData(state), [getTopicData]));
};
// log tab
export const useDevToolLog = () => {
  const getLog = useMemo(selectors.makeGetDevToolLog, []);
  return useSelector(useCallback(state => getLog(state), [getLog]));
};
