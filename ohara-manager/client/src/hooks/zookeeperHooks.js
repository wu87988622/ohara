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
import { merge } from 'lodash';

import * as hooks from 'hooks';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { getId } from 'utils/object';

export const useZookeeperGroup = () => 'zookeeper';

export const useZookeeperName = () => {
  const workspace = hooks.useWorkspaceName();
  return workspace;
};

export const useFetchZookeeperAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useZookeeperGroup();
  return useCallback(
    name => dispatch(actions.fetchZookeeper.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useCreateZookeeperAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useZookeeperGroup();
  return useCallback(
    values => dispatch(actions.createZookeeper.trigger({ ...values, group })),
    [dispatch, group],
  );
};

export const useUpdateZookeeperAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useZookeeperGroup();
  return useCallback(
    values => dispatch(actions.updateZookeeper.trigger({ ...values, group })),
    [dispatch, group],
  );
};

export const useStartZookeeperAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useZookeeperGroup();
  return useCallback(
    name => dispatch(actions.startZookeeper.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useStopZookeeperAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useZookeeperGroup();
  return useCallback(
    name => dispatch(actions.stopZookeeper.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useDeleteZookeeperAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useZookeeperGroup();
  return useCallback(
    name => dispatch(actions.deleteZookeeper.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useZookeepers = () => {
  const getAllZookeepers = useMemo(selectors.makeGetAllZookeepers, []);
  const zookeepers = useSelector(
    useCallback(state => getAllZookeepers(state), [getAllZookeepers]),
  );
  return zookeepers;
};

export const useZookeeper = () => {
  const getZookeeperById = useMemo(selectors.makeGetZookeeperById, []);
  const getInfoById = useMemo(selectors.makeGetInfoById, []);
  const group = useZookeeperGroup();
  const name = useZookeeperName();
  const id = getId({ group, name });
  return useSelector(
    useCallback(
      state => {
        const zookeeper = getZookeeperById(state, { id });
        const info = getInfoById(state, { id });
        return merge(zookeeper, info);
      },
      [getZookeeperById, getInfoById, id],
    ),
  );
};
