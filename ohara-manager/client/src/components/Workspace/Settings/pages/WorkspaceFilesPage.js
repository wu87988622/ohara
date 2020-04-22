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

import React from 'react';

import { FileTable } from 'components/File';
import * as hooks from 'hooks';

function WorkspaceFilesPage() {
  const files = hooks.useFiles();
  const createFile = hooks.useCreateFileAction();
  const deleteFile = hooks.useDeleteFileAction();

  const handleDelete = file => {
    deleteFile(file?.name);
  };

  const handleUpload = event => {
    const file = event?.target?.files?.[0];
    if (file) createFile(file);
  };

  return (
    <>
      <FileTable
        files={files}
        onDelete={handleDelete}
        onUpload={handleUpload}
        title="Workspace files"
      />
    </>
  );
}

export default WorkspaceFilesPage;
