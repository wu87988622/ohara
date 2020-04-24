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
import { capitalize, join, includes, some, toUpper } from 'lodash';

import { FileTable } from 'components/File';
import * as hooks from 'hooks';

const WORKER = 'worker';
const STREAM = 'stream';

function WorkspaceFilesPage() {
  const files = hooks.useFiles();
  const createFile = hooks.useCreateFileAction();
  const deleteFile = hooks.useDeleteFileAction();

  const worker = hooks.useWorker();
  const workspace = hooks.useWorkspace();

  const handleDelete = file => {
    deleteFile(file?.name);
  };

  const handleUpload = event => {
    const file = event?.target?.files?.[0];
    if (file) createFile(file);
  };

  const isUsedByWorker = file => {
    return (
      some(worker?.pluginKeys, key => key.name === file?.name) ||
      some(worker?.sharedJarKeys, key => key.name === file?.name) ||
      some(workspace?.worker?.pluginKeys, key => key.name === file?.name) ||
      some(workspace?.worker?.sharedJarKeys, key => key.name === file?.name)
    );
  };

  const isUsedByStream = file => {
    return some(workspace?.stream?.jarKeys, key => key.name === file?.name);
  };

  return (
    <>
      <FileTable
        files={files}
        onDelete={handleDelete}
        onUpload={handleUpload}
        options={{
          customColumns: [
            {
              title: 'Used',
              customFilterAndSearch: (filterValue, file) => {
                const value = [];
                if (isUsedByWorker(file)) value.push(WORKER);
                if (isUsedByStream(file)) value.push(STREAM);
                return includes(toUpper(join(value)), toUpper(filterValue));
              },
              render: file => {
                return (
                  <>
                    {isUsedByWorker(file) && <div>{capitalize(WORKER)}</div>}
                    {isUsedByStream(file) && <div>{capitalize(STREAM)}</div>}
                  </>
                );
              },
            },
          ],
          disabledDeleteIcon: file => {
            return isUsedByWorker(file) || isUsedByStream(file);
          },
        }}
        title="Workspace files"
      />
    </>
  );
}

export default WorkspaceFilesPage;
