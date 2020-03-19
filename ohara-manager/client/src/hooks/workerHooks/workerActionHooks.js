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

import { useDispatch } from 'redux-react-hook';
import { useApp } from 'context';
import * as actions from 'store/actions';

export const useFetchWorkerAction = () => {
  const dispatch = useDispatch();
  const { workerGroup } = useApp();
  return function(name) {
    const params = { name, group: workerGroup };
    dispatch(actions.fetchWorker.trigger(params));
  };
};

export const useCreateWorkerAction = () => {
  const dispatch = useDispatch();
  const { workerGroup } = useApp();
  return function(values) {
    const finalValues = { ...values, group: workerGroup };
    dispatch(actions.createWorker.trigger(finalValues));
  };
};

export const useUpdateWorkerAction = () => {
  const dispatch = useDispatch();
  const { workerGroup } = useApp();
  return function(values) {
    const finalValues = { ...values, group: workerGroup };
    dispatch(actions.updateWorker.trigger(finalValues));
  };
};

export const useStartWorkerAction = () => {
  const dispatch = useDispatch();
  const { brokerGroup } = useApp();
  return function(name) {
    const params = { name, group: brokerGroup };
    dispatch(actions.startWorker.trigger(params));
  };
};

export const useStopWorkerAction = () => {
  const dispatch = useDispatch();
  const { brokerGroup } = useApp();
  return function(name) {
    const params = { name, group: brokerGroup };
    dispatch(actions.stopWorker.trigger(params));
  };
};

export const useDeleteWorkerAction = () => {
  const dispatch = useDispatch();
  const { workerGroup } = useApp();
  return function(name) {
    const params = { name, group: workerGroup };
    dispatch(actions.deleteWorker.trigger(params));
  };
};
