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

import { pick, map } from 'lodash';

import * as workerApi from 'api/workerApi';
import * as inspectApi from 'api/inspectApi';
import * as objectApi from 'api/objectApi';
import {
  fetchWorkersRoutine,
  updateStagingSettingsRoutine,
} from './workerRoutines';

const fetchWorkersCreator = (
  state,
  dispatch,
  showMessage,
  routine = fetchWorkersRoutine,
) => async () => {
  if (state.isFetching || state.lastUpdated || state.error) return;

  dispatch(routine.request());
  const result = await workerApi.getAll();

  if (result.errors) {
    dispatch(routine.failure(result.title));
    showMessage(result.title);
    return;
  }

  const workers = result.data;
  const workerInfos = await Promise.all(
    map(workers, async worker => {
      const params = pick(worker.settings, ['name', 'group']);
      const result = await inspectApi.getWorkerInfo(params);
      const workerInfo = result.errors ? {} : result.data;

      const result2 = await objectApi.get(params);
      const stagingSettings = result2.errors ? {} : result2.data;

      return {
        serviceType: 'worker',
        ...worker,
        ...workerInfo,
        stagingSettings,
      };
    }),
  );

  dispatch(routine.success(workerInfos));
};

const addWorkerCreator = () => async () => {
  // TODO: implement the logic for add worker
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
