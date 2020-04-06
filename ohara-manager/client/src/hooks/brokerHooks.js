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

import { useCallback, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { merge } from 'lodash';

import { GROUP } from 'const';
import * as hooks from 'hooks';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { getId } from 'utils/object';

export const useBrokerGroup = () => GROUP.BROKER;

export const useBrokerName = () => {
  const brokerName = hooks.useWorkspaceName();
  return brokerName;
};

export const useBrokerId = () => {
  const group = hooks.useBrokerGroup();
  const name = hooks.useBrokerName();
  return getId({ group, name });
};

export const useIsBrokerLoaded = () => {
  const brokerId = hooks.useBrokerId();
  const selector = useCallback(
    state => selectors.isBrokerLoaded(state, { id: brokerId }),
    [brokerId],
  );
  return useSelector(selector);
};

export const useIsBrokerLoading = () => {
  const brokerId = hooks.useBrokerId();
  const selector = useCallback(
    state => selectors.isBrokerLoading(state, { id: brokerId }),
    [brokerId],
  );
  return useSelector(selector);
};

export const useBroker = () => {
  const name = hooks.useBrokerName();
  const brokerId = hooks.useBrokerId();
  const isLoaded = hooks.useIsBrokerLoaded();
  const fetchBroker = hooks.useFetchBrokerAction();

  useEffect(() => {
    if (!isLoaded && name) fetchBroker(name);
  }, [isLoaded, fetchBroker, name]);

  const selector = useCallback(
    state =>
      merge(
        selectors.getBrokerById(state, { id: brokerId }),
        selectors.getInfoById(state, { id: brokerId }),
      ),
    [brokerId],
  );
  return useSelector(selector);
};

export const useAllBrokers = () => {
  const selector = useCallback(state => selectors.getAllBrokers(state), []);
  return useSelector(selector);
};

export const useFetchBrokerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useBrokerGroup();
  return useCallback(
    name => dispatch(actions.fetchBroker.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useCreateBrokerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useBrokerGroup();
  return useCallback(
    values => dispatch(actions.createBroker.trigger({ ...values, group })),
    [dispatch, group],
  );
};

export const useUpdateBrokerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useBrokerGroup();
  return useCallback(
    values => dispatch(actions.updateBroker.trigger({ ...values, group })),
    [dispatch, group],
  );
};

export const useStartBrokerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useBrokerGroup();
  return useCallback(
    name => dispatch(actions.startBroker.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useStopBrokerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useBrokerGroup();
  return useCallback(
    name => dispatch(actions.stopBroker.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useDeleteBrokerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useBrokerGroup();
  return useCallback(
    name => dispatch(actions.deleteBroker.trigger({ group, name })),
    [dispatch, group],
  );
};
