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

export const useWorkerGroup = () => GROUP.WORKER;

export const useWorkerName = () => {
  const workerName = hooks.useWorkspaceName();
  return workerName;
};

export const useWorkerId = () => {
  const group = hooks.useWorkerGroup();
  const name = hooks.useWorkerName();
  return getId({ group, name });
};

export const useIsWorkerLoaded = () => {
  const workerId = hooks.useWorkerId();
  const selector = useCallback(
    (state) => selectors.isWorkerLoaded(state, { id: workerId }),
    [workerId],
  );
  return useSelector(selector);
};

export const useIsWorkerLoading = () => {
  const workerId = hooks.useWorkerId();
  const selector = useCallback(
    (state) => selectors.isWorkerLoading(state, { id: workerId }),
    [workerId],
  );
  return useSelector(selector);
};

export const useWorker = () => {
  const name = hooks.useWorkerName();
  const workerId = hooks.useWorkerId();
  const isLoaded = hooks.useIsWorkerLoaded();
  const fetchWorker = hooks.useFetchWorkerAction();

  useEffect(() => {
    if (!isLoaded && name) fetchWorker(name);
  }, [isLoaded, fetchWorker, name]);

  const selector = useCallback(
    (state) =>
      merge(
        selectors.getWorkerById(state, { id: workerId }),
        selectors.getInfoById(state, { id: workerId }),
      ),
    [workerId],
  );
  return useSelector(selector);
};

export const useAllWorkers = () => {
  const selector = useCallback((state) => selectors.getAllWorkers(state), []);
  return useSelector(selector);
};

export const useFetchWorkerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useWorkerGroup();
  return useCallback(
    (name) => dispatch(actions.fetchWorker.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useCreateWorkerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useWorkerGroup();
  return useCallback(
    (values) =>
      new Promise((resolve, reject) =>
        dispatch(
          actions.createWorker.trigger({
            values: { ...values, group },
            resolve,
            reject,
          }),
        ),
      ),
    [dispatch, group],
  );
};

export const useUpdateWorkerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useWorkerGroup();
  return useCallback(
    (values) => dispatch(actions.updateWorker.trigger({ ...values, group })),
    [dispatch, group],
  );
};

export const useStartWorkerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useWorkerGroup();
  return useCallback(
    (name) =>
      new Promise((resolve, reject) =>
        dispatch(
          actions.startWorker.trigger({
            values: { group, name },
            resolve,
            reject,
          }),
        ),
      ),
    [dispatch, group],
  );
};

export const useStopWorkerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useWorkerGroup();
  return useCallback(
    (name) =>
      new Promise((resolve, reject) =>
        dispatch(
          actions.stopWorker.trigger({
            values: { group, name },
            resolve,
            reject,
          }),
        ),
      ),
    [dispatch, group],
  );
};

export const useDeleteWorkerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useWorkerGroup();
  return useCallback(
    (name) =>
      new Promise((resolve, reject) =>
        dispatch(
          actions.deleteWorker.trigger({
            values: { group, name },
            resolve,
            reject,
          }),
        ),
      ),
    [dispatch, group],
  );
};

export const useWorkerClusterKey = () => {
  const group = useWorkerGroup();
  const name = useWorkerName();
  return {
    name,
    group,
  };
};
