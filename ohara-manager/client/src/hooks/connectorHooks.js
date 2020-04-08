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

import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import * as hooks from 'hooks';
import { hashByGroupAndName } from 'utils/sha';

export const useCreateConnectorAction = () => {
  const dispatch = useDispatch();
  const group = useConnectorGroup();
  return (values, options) =>
    dispatch(
      actions.createConnector.trigger({
        params: { ...values, group },
        options,
      }),
    );
};

export const useUpdateConnectorAction = () => {
  const dispatch = useDispatch();
  const group = useConnectorGroup();
  return (values, options) =>
    dispatch(
      actions.updateConnector.trigger({
        params: { ...values, group },
        options,
      }),
    );
};

export const useUpdateConnectorLinkAction = () => {
  const dispatch = useDispatch();
  const group = useConnectorGroup();
  return (values, options) =>
    dispatch(
      actions.updateConnectorLink.trigger({
        params: { ...values, group },
        options,
      }),
    );
};

export const useRemoveSourceLinkAction = () => {
  const dispatch = useDispatch();
  const group = useConnectorGroup();
  return (values, options) =>
    dispatch(
      actions.removeConnectorSourceLink.trigger({
        params: { ...values, group },
        options,
      }),
    );
};

export const useRemoveSinkLinkAction = () => {
  const dispatch = useDispatch();
  const group = useConnectorGroup();
  return (values, options) =>
    dispatch(
      actions.removeConnectorSinkLink.trigger({
        params: { ...values, group },
        options,
      }),
    );
};

export const useStartConnectorAction = () => {
  const dispatch = useDispatch();
  const group = useConnectorGroup();
  return (values, options) =>
    dispatch(
      actions.startConnector.trigger({
        params: { ...values, group },
        options,
      }),
    );
};

export const useStopConnectorAction = () => {
  const dispatch = useDispatch();
  const group = useConnectorGroup();
  return (values, options) =>
    dispatch(
      actions.stopConnector.trigger({
        params: { ...values, group },
        options,
      }),
    );
};

export const useDeleteConnectorAction = () => {
  const dispatch = useDispatch();
  const group = useConnectorGroup();
  return (values, options) =>
    dispatch(
      actions.deleteConnector.trigger({
        params: { ...values, group },
        options,
      }),
    );
};

export const useFetchConnectorsAction = () => {
  const dispatch = useDispatch();
  const group = useConnectorGroup();
  return () =>
    dispatch(
      actions.fetchConnectors.trigger({
        group,
      }),
    );
};

export const useIsConnectorLoaded = () => {
  const mapState = useCallback(state => !!state.ui.connector?.lastUpdated, []);
  return useSelector(mapState);
};

export const useIsConnectorLoading = () => {
  const mapState = useCallback(state => !!state.ui.connector?.loading, []);
  return useSelector(mapState);
};

export const useConnectorGroup = () => {
  const pipelineGroup = hooks.usePipelineGroup();
  const pipelineName = hooks.usePipelineName();
  if (pipelineGroup && pipelineName)
    return hashByGroupAndName(pipelineGroup, pipelineName);
};

export const useConnectors = () => {
  const getConnectorByGroup = useMemo(
    selectors.makeGetAllConnectorsByGroup,
    [],
  );
  const group = useConnectorGroup();
  const fetchConnectors = useFetchConnectorsAction();

  useEffect(() => {
    if (useIsConnectorLoaded || useIsConnectorLoading) return;

    fetchConnectors();
  }, [fetchConnectors]);

  return useSelector(
    useCallback(state => getConnectorByGroup(state, { group }), [
      getConnectorByGroup,
      group,
    ]),
  );
};
