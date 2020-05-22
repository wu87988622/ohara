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

import { merge, isEmpty } from 'lodash';
import { useCallback, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';

import * as hooks from 'hooks';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { hashByGroupAndName } from 'utils/sha';

export const useIsStreamLoaded = () => {
  const mapState = useCallback(state => !!state.ui.stream?.lastUpdated, []);
  return useSelector(mapState);
};

export const useIsStreamLoading = () => {
  const mapState = useCallback(state => !!state.ui.stream?.loading, []);
  return useSelector(mapState);
};

export const useStreamGroup = () => {
  const usePipelineGroup = hooks.usePipelineGroup();
  const pipelineName = hooks.usePipelineName();
  if (usePipelineGroup && pipelineName)
    return hashByGroupAndName(usePipelineGroup, pipelineName);
};

export const useCreateStreamAction = () => {
  const dispatch = useDispatch();
  const group = useStreamGroup();
  const brokerClusterKey = {
    group: hooks.useBrokerGroup(),
    name: hooks.useWorkspaceName(),
  };

  return useCallback(
    (values, options) => {
      const newValues = { ...values, group, brokerClusterKey };
      dispatch(actions.createStream.trigger({ values: newValues, options }));
    },
    [brokerClusterKey, dispatch, group],
  );
};

export const useFetchStreamsAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.fetchStreams.trigger()), [
    dispatch,
  ]);
};

export const useUpdateStreamAction = () => {
  const dispatch = useDispatch();
  const group = useStreamGroup();

  return useCallback(
    (values, options) => {
      const newValues = { ...values, group };
      dispatch(actions.updateStream.trigger({ values: newValues, options }));
    },
    [dispatch, group],
  );
};

export const useDeleteStreamAction = () => {
  const dispatch = useDispatch();
  const group = useStreamGroup();
  return useCallback(
    (params, options) => {
      const newParams = { ...params, group };
      dispatch(actions.deleteStream.trigger({ params: newParams, options }));
    },
    [dispatch, group],
  );
};

export const useStartStreamAction = () => {
  const dispatch = useDispatch();
  const group = useStreamGroup();
  return useCallback(
    (params, options) => {
      const newParams = { ...params, group };
      dispatch(actions.startStream.trigger({ params: newParams, options }));
    },
    [dispatch, group],
  );
};

export const useStopStreamAction = () => {
  const dispatch = useDispatch();
  const group = useStreamGroup();
  return useCallback(
    (params, options) => {
      const newParams = { ...params, group };
      dispatch(actions.stopStream.trigger({ params: newParams, options }));
    },
    [dispatch, group],
  );
};

export const useStreams = () => {
  const group = useStreamGroup();
  const fetchStreams = useFetchStreamsAction();
  const isStreamLoaded = useIsStreamLoaded();
  const isStreamLoading = useIsStreamLoading();
  const isAppReady = hooks.useIsAppReady();

  useEffect(() => {
    if (isStreamLoaded || isStreamLoading || !isAppReady) return;
    fetchStreams();
  }, [fetchStreams, isAppReady, isStreamLoaded, isStreamLoading]);

  return useSelector(state => {
    const streams = selectors.getStreamByGroup(state, { group });
    const results = streams.map(stream => {
      const { stream__class: className, jarKey } = stream;
      const info = selectors.getStreamInfo(state, { jarKey, className });
      const settingDefinitions = info?.settingDefinitions || [];
      return merge(stream, { settingDefinitions });
    });
    return results;
  });
};

export const useRemoveStreamToLinkAction = () => {
  const dispatch = useDispatch();
  const group = useStreamGroup();
  return useCallback(
    (params, options) => {
      const newParams = { ...params, group };
      dispatch(
        actions.removeStreamToLink.trigger({ params: newParams, options }),
      );
    },
    [dispatch, group],
  );
};

export const useRemoveStreamFromLinkAction = () => {
  const dispatch = useDispatch();
  const group = useStreamGroup();
  return useCallback(
    (params, options) => {
      const newParams = { ...params, group };
      dispatch(
        actions.removeStreamFromLink.trigger({ params: newParams, options }),
      );
    },
    [dispatch, group],
  );
};

export const useUpdateStreamLinkAction = () => {
  const dispatch = useDispatch();
  const streamGroup = useStreamGroup();
  const topicGroup = hooks.useTopicGroup();

  return useCallback(
    (values, options) => {
      let newValues = {};
      if (!isEmpty(values.from)) {
        newValues = {
          ...values,
          from: values.from.map(t => ({ ...t, group: topicGroup })),
          group: streamGroup,
        };
      } else if (!isEmpty(values.to)) {
        newValues = {
          ...values,
          to: values.to.map(t => ({ ...t, group: topicGroup })),
          group: streamGroup,
        };
      }

      return dispatch(
        actions.updateStreamLink.trigger({
          values: newValues,
          options,
        }),
      );
    },
    [dispatch, streamGroup, topicGroup],
  );
};

export const useStopAndDeleteStreamAction = () => {
  const dispatch = useDispatch();
  const group = useStreamGroup();
  return useCallback(
    (params, options) =>
      dispatch(
        actions.stopAndDeleteStream.trigger({
          params: { ...params, group },
          options,
        }),
      ),
    [dispatch, group],
  );
};
