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

export const useZookeeperGroup = () => GROUP.ZOOKEEPER;

export const useZookeeperName = () => {
  const zookeeperName = hooks.useWorkspaceName();
  return zookeeperName;
};

export const useZookeeperId = () => {
  const group = hooks.useZookeeperGroup();
  const name = hooks.useZookeeperName();
  return getId({ group, name });
};

export const useIsZookeeperLoaded = () => {
  const zookeeperId = hooks.useZookeeperId();
  const selector = useCallback(
    (state) => selectors.isZookeeperLoaded(state, { id: zookeeperId }),
    [zookeeperId],
  );
  return useSelector(selector);
};

export const useIsZookeeperLoading = () => {
  const zookeeperId = hooks.useZookeeperId();
  const selector = useCallback(
    (state) => selectors.isZookeeperLoading(state, { id: zookeeperId }),
    [zookeeperId],
  );
  return useSelector(selector);
};

export const useZookeeper = () => {
  const name = hooks.useZookeeperName();
  const zookeeperId = hooks.useZookeeperId();
  const isLoaded = hooks.useIsZookeeperLoaded();
  const fetchZookeeper = hooks.useFetchZookeeperAction();

  useEffect(() => {
    if (!isLoaded && name) fetchZookeeper(name);
  }, [isLoaded, fetchZookeeper, name]);

  const selector = useCallback(
    (state) =>
      merge(
        selectors.getZookeeperById(state, { id: zookeeperId }),
        selectors.getInfoById(state, { id: zookeeperId }),
      ),
    [zookeeperId],
  );
  return useSelector(selector);
};

export const useAllZookeepers = () => {
  const selector = useCallback(
    (state) => selectors.getAllZookeepers(state),
    [],
  );
  return useSelector(selector);
};

export const useFetchZookeeperAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useZookeeperGroup();
  return useCallback(
    (name) => dispatch(actions.fetchZookeeper.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useCreateZookeeperAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useZookeeperGroup();
  return useCallback(
    (values) =>
      new Promise((resolve, reject) =>
        dispatch(
          actions.createZookeeper.trigger({
            values: { ...values, group },
            resolve,
            reject,
          }),
        ),
      ),
    [dispatch, group],
  );
};

export const useUpdateZookeeperAction = () => {
  const dispatch = useDispatch();
  return useCallback(
    (values) =>
      new Promise((resolve, reject) =>
        dispatch(actions.updateZookeeper.trigger({ values, resolve, reject })),
      ),
    [dispatch],
  );
};

export const useStartZookeeperAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useZookeeperGroup();
  return useCallback(
    (name) =>
      new Promise((resolve, reject) =>
        dispatch(
          actions.startZookeeper.trigger({
            values: { group, name },
            resolve,
            reject,
          }),
        ),
      ),
    [dispatch, group],
  );
};

export const useStopZookeeperAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useZookeeperGroup();
  return useCallback(
    (name) =>
      new Promise((resolve, reject) =>
        dispatch(
          actions.stopZookeeper.trigger({
            values: { group, name },
            resolve,
            reject,
          }),
        ),
      ),
    [dispatch, group],
  );
};

export const useDeleteZookeeperAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useZookeeperGroup();
  return useCallback(
    (name) =>
      new Promise((resolve, reject) =>
        dispatch(
          actions.deleteZookeeper.trigger({
            values: { group, name },
            resolve,
            reject,
          }),
        ),
      ),
    [dispatch, group],
  );
};
