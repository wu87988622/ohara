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

import { some } from 'lodash';
import * as routines from './fileRoutines';

export const createActions = context => {
  const { state, dispatch, fileApi } = context;
  return {
    fetchFiles: async () => {
      const routine = routines.fetchFilesRoutine;
      if (state.isFetching || state.lastUpdated || state.error) return;
      try {
        dispatch(routine.request());
        const data = await fileApi.fetchAll();
        dispatch(routine.success(data));
      } catch (e) {
        dispatch(routine.failure(e.message));
      }
    },
    createFile: async file => {
      const routine = routines.createFileRoutine;
      if (state.isFetching) return;
      try {
        const { data: files } = state;
        // check duplicate
        if (some(files, { name: file.name })) {
          throw new Error(`The file name ${file.name} already exists!`);
        }
        dispatch(routine.request());
        const data = await fileApi.create(file);
        dispatch(routine.success(data));
      } catch (e) {
        dispatch(routine.failure(e.message));
      }
    },
    deleteFile: async name => {
      const routine = routines.deleteFileRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await fileApi.delete(name);
        dispatch(routine.success(data));
      } catch (e) {
        dispatch(routine.failure(e.message));
      }
    },
  };
};
