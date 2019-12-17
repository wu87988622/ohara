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

import { has, map } from 'lodash';

import * as workerApi from 'api/workerApi';
import * as inspectApi from 'api/inspectApi';
import * as objectApi from 'api/objectApi';
import {
  fetchWorkersRoutine,
  addWorkerRoutine,
  updateStagingSettingsRoutine,
} from './workerRoutines';
import { getKey } from 'utils/object';

const WORKER = 'worker';

const checkRequired = values => {
  if (!has(values, 'name')) {
    throw new Error("Values is missing required member 'name'");
  }
};

const transformToWorker = (worker, workerInfo, stagingWorker) => {
  if (!has(worker, 'settings')) {
    throw new Error("worker is missing required member 'settings'");
  }
  if (!has(workerInfo, 'settingDefinitions')) {
    throw new Error(
      "workerInfo is missing required member 'settingDefinitions'",
    );
  }
  return {
    serviceType: WORKER,
    ...worker,
    ...workerInfo,
    stagingSettings: stagingWorker,
  };
};

const fetchWorkersCreator = (
  state,
  dispatch,
  showMessage,
  routine = fetchWorkersRoutine,
) => async () => {
  if (state.isFetching || state.lastUpdated || state.error) return;

  try {
    dispatch(routine.request());

    const resultForFetchWorkers = await workerApi.getAll();
    if (resultForFetchWorkers.errors) {
      throw new Error(resultForFetchWorkers.title);
    }

    const workers = await Promise.all(
      map(resultForFetchWorkers.data, async worker => {
        const key = getKey(worker);

        const resultForFetchWorkerInfo = await inspectApi.getWorkerInfo(key);
        if (resultForFetchWorkerInfo.errors) {
          throw new Error(resultForFetchWorkerInfo.title);
        }

        const resultForFetchStagingWorker = await objectApi.get(key);
        if (resultForFetchStagingWorker.errors) {
          throw new Error(resultForFetchStagingWorker.title);
        }

        return transformToWorker(
          worker,
          resultForFetchWorkerInfo.data,
          resultForFetchStagingWorker.data,
        );
      }),
    );
    dispatch(routine.success(workers));
  } catch (e) {
    dispatch(routine.failure(e.message));
    showMessage(e.message);
  }
};

const addWorkerCreator = (
  state,
  dispatch,
  showMessage,
  routine = addWorkerRoutine,
) => async values => {
  if (state.isFetching) return;

  try {
    checkRequired(values);
    const ensuredValues = { ...values, group: WORKER };
    dispatch(routine.request());

    const resultForCreateWorker = await workerApi.create(ensuredValues);
    if (resultForCreateWorker.errors) {
      throw new Error(resultForCreateWorker.title);
    }

    const resultForStartWorker = await workerApi.start(ensuredValues);
    if (resultForStartWorker.errors) {
      throw new Error(resultForStartWorker.title);
    }

    const resultForStageWorker = await objectApi.create(ensuredValues);
    if (resultForStageWorker.errors) {
      throw new Error(resultForStageWorker.title);
    }

    const resultForFetchWorkerInfo = await inspectApi.getWorkerInfo(
      ensuredValues,
    );
    if (resultForFetchWorkerInfo.errors) {
      throw new Error(resultForFetchWorkerInfo.title);
    }

    dispatch(
      routine.success(
        transformToWorker(
          resultForCreateWorker.data,
          resultForFetchWorkerInfo.data,
          resultForStageWorker.data,
        ),
      ),
    );
  } catch (e) {
    dispatch(routine.failure(e.message));
    showMessage(e.message);
  }
};

const updateWorkerCreator = () => async () => {
  // TODO: implement the logic for update worker
};

const deleteWorkerCreator = () => async () => {
  // TODO: implement the logic for delete worker
};

const updateStagingSettingsCreator = (
  state,
  dispatch,
  showMessage,
  routine = updateStagingSettingsRoutine,
) => async params => {
  if (state.isFetching) return;

  dispatch(routine.request());
  const result = await objectApi.update(params);

  if (result.errors) {
    dispatch(routine.failure(result.title));
    showMessage(result.title);
    return;
  }

  dispatch(routine.success(result.data));
};

export {
  fetchWorkersCreator,
  addWorkerCreator,
  updateWorkerCreator,
  deleteWorkerCreator,
  updateStagingSettingsCreator,
};
