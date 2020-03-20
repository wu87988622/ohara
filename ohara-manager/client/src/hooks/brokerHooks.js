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
import { merge } from 'lodash';

import * as hooks from 'hooks';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { getId } from 'utils/object';

export const useBrokerGroup = () => 'broker';

export const useBrokerName = () => {
  const workspace = hooks.useWorkspaceName();
  return workspace;
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

export const useBrokers = () => {
  const getBrokers = useMemo(selectors.makeGetBrokers, []);
  return useMappedState(useCallback(state => getBrokers(state), [getBrokers]));
};

export const useBroker = () => {
  const getBrokerById = useMemo(selectors.makeGetBrokerById, []);
  const getInfoById = useMemo(selectors.makeGetInfoById, []);
  const group = useBrokerGroup();
  const name = useBrokerName();
  const id = getId({ group, name });
  return useMappedState(
    useCallback(
      state => {
        const broker = getBrokerById(state, { id });
        const info = getInfoById(state, { id });
        return merge(broker, info);
      },
      [getBrokerById, getInfoById, id],
    ),
  );
};
