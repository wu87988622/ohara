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

import { isEmpty, some } from 'lodash';

import * as fileApi from 'api/fileApi';
import {
  fetchFilesRoutine,
  uploadFileRoutine,
  deleteFileRoutine,
} from './fileRoutines';

const createFetchFiles = (state, dispatch) => async workspaceName => {
  if (state.isFetching || state.lastUpdated || state.error) return;

  dispatch(fetchFilesRoutine.request());
  const files = await fileApi.getAll({ group: workspaceName });

  if (isEmpty(files)) {
    dispatch(fetchFilesRoutine.failure('failed to fetch files'));
    return;
  }

  dispatch(fetchFilesRoutine.success(files));
};

const createUploadFile = (state, dispatch, showMessage) => async (
  workspaceName,
  file,
) => {
  if (state.isFetching) return;

  const { data: files } = state;
  const isDuplicate = () => some(files, { name: file.name });

  if (isDuplicate()) {
    showMessage(`The file name ${file.name} already exists!`);
    return;
  }

  dispatch(uploadFileRoutine.request());
  const createFileResponse = await fileApi.create({
    group: workspaceName,
    file,
  });

  const isCreated = !isEmpty(createFileResponse);

  // Failed to upload, show a custom error message
  if (!isCreated) {
    const error = `Failed to upload file ${file.name}`;
    dispatch(uploadFileRoutine.failure(error));
    showMessage(error);
    return;
  }

  // File successfully uploaded, display success message
  dispatch(uploadFileRoutine.success(createFileResponse));
  showMessage(`Successfully uploaded file ${file.name}`);
};

const createDeleteFile = (state, dispatch, showMessage) => async (
  name,
  group,
) => {
  if (state.isFetching) return;

  dispatch(deleteFileRoutine.request());
  const deleteFileResponse = await fileApi.remove({
    name,
    group,
  });

  const isDeleted = isEmpty(deleteFileResponse);

  if (!isDeleted) {
    const error = `Failed to delete file ${name}`;
    dispatch(deleteFileRoutine.failure(error));
    showMessage(error);
    return;
  }

  dispatch(
    deleteFileRoutine.success({
      name,
      group,
    }),
  );
  showMessage(`Successfully deleted file ${name}`);
};

export { createFetchFiles, createUploadFile, createDeleteFile };
