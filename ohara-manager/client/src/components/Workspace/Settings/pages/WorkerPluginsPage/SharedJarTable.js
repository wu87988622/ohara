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

import React, { useState, useRef } from 'react';
import { every, filter, find, map, some, reject } from 'lodash';

import { FileSelectorDialog, FileTable } from 'components/File';
import * as hooks from 'hooks';
import { getKey } from 'utils/object';

function SharedJarTable() {
  const files = hooks.useFiles();
  const worker = hooks.useWorker();
  const workspace = hooks.useWorkspace();
  const updateWorkspace = hooks.useUpdateWorkspaceAction();
  const createFile = hooks.useCreateFileAction();

  const selectorDialogRef = useRef(null);
  const [isSelectorDialogOpen, setIsSelectorDialogOpen] = useState(false);

  const workerSharedJars = filter(
    map(worker?.sharedJarKeys, sharedJarKey =>
      find(files, file => file.name === sharedJarKey.name),
    ),
  );

  const workspaceSharedJars = workspace?.worker?.sharedJarKeys
    ? filter(
        map(workspace.worker.sharedJarKeys, sharedJarKey =>
          find(files, file => file.name === sharedJarKey.name),
        ),
      )
    : workerSharedJars;

  const updateSharedJarKeysToWorkspace = newSharedJarKeys => {
    updateWorkspace({
      ...workspace,
      worker: {
        ...workspace?.worker,
        sharedJarKeys: newSharedJarKeys,
      },
    });

    const newSelectedFiles = filter(
      map(newSharedJarKeys, sharedJarKey =>
        find(files, file => file.name === sharedJarKey.name),
      ),
    );

    selectorDialogRef.current.setSelectedFiles(newSelectedFiles);
  };

  const handleAddIconClick = () => {
    setIsSelectorDialogOpen(true);
  };

  const handleRemove = fileToRemove => {
    if (!fileToRemove?.group || !fileToRemove?.name) return;

    const shouldBeRemoved = some(
      workspace?.worker?.sharedJarKeys,
      sharedJarKey => sharedJarKey.name === fileToRemove.name,
    );

    if (shouldBeRemoved) {
      const newSharedJarKeys = reject(
        workspace?.worker?.sharedJarKeys,
        sharedJarKey => sharedJarKey.name === fileToRemove.name,
      );
      updateSharedJarKeysToWorkspace(newSharedJarKeys);
    }
  };

  const handleSelectorDialogConfirm = selectedFiles => {
    const newSharedJarKeys = map(selectedFiles, file => getKey(file));
    updateSharedJarKeysToWorkspace(newSharedJarKeys);
    setIsSelectorDialogOpen(false);
  };

  const handleUpload = event => {
    const file = event?.target?.files?.[0];
    if (file) createFile(file);
  };

  const handleUndoIconClick = fileClicked => {
    if (!fileClicked?.group || !fileClicked?.name) return;

    const shouldBeAdded = every(
      workspace?.worker?.sharedJarKeys,
      sharedJarKey => sharedJarKey.name !== fileClicked.name,
    );

    if (shouldBeAdded) {
      const newSharedJarKeys = [
        ...workspace?.worker?.sharedJarKeys,
        getKey(fileClicked),
      ];
      updateSharedJarKeysToWorkspace([newSharedJarKeys]);
    } else {
      handleRemove(fileClicked);
    }
  };

  return (
    <>
      <FileTable
        files={workspaceSharedJars}
        onRemove={handleRemove}
        options={{
          comparison: true,
          comparedNodes: workerSharedJars,
          onAddIconClick: handleAddIconClick,
          onUndoIconClick: handleUndoIconClick,
          showAddIcon: true,
          showUploadIcon: false,
          showDeleteIcon: false,
          showDownloadIcon: false,
          showRemoveIcon: true,
        }}
        title="Shared jars"
      />

      <FileSelectorDialog
        isOpen={isSelectorDialogOpen}
        files={files}
        onClose={() => setIsSelectorDialogOpen(false)}
        onConfirm={handleSelectorDialogConfirm}
        onUpload={handleUpload}
        ref={selectorDialogRef}
        selectedFiles={workerSharedJars}
        tableTitle="Workspace files"
      />
    </>
  );
}

export default SharedJarTable;
