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

export const useWorkerGroup = () => 'worker';

export const useWorkerName = () => {
  const workspace = hooks.useWorkspaceName();
  return workspace;
};

export const useFetchWorkerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useWorkerGroup();
  return useCallback(
    name => dispatch(actions.fetchWorker.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useCreateWorkerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useWorkerGroup();
  return useCallback(
    values => dispatch(actions.createWorker.trigger({ ...values, group })),
    [dispatch, group],
  );
};

export const useUpdateWorkerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useWorkerGroup();
  return useCallback(
    values => dispatch(actions.updateWorker.trigger({ ...values, group })),
    [dispatch, group],
  );
};

export const useStartWorkerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useWorkerGroup();
  return useCallback(
    name => dispatch(actions.startWorker.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useStopWorkerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useWorkerGroup();
  return useCallback(
    name => dispatch(actions.stopWorker.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useDeleteWorkerAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useWorkerGroup();
  return useCallback(
    name => dispatch(actions.deleteWorker.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useWorkers = () => {
  const getAllWorkers = useMemo(selectors.makeGetAllWorkers, []);
  const workers = useSelector(
    useCallback(state => getAllWorkers(state), [getAllWorkers]),
  );
  return workers;
};

export const useWorker = () => {
  const getWorkerById = useMemo(selectors.makeGetWorkerById, []);
  const getInfoById = useMemo(selectors.makeGetInfoById, []);
  const group = useWorkerGroup();
  const name = useWorkerName();
  const id = getId({ group, name });
  return useSelector(
    useCallback(
      state => {
        const worker = getWorkerById(state, { id });
        const info = getInfoById(state, { id });
        return merge(worker, info);
      },
      [getWorkerById, getInfoById, id],
    ),
  );
};
