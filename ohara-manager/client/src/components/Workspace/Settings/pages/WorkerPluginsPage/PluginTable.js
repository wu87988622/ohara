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

function PluginTable() {
  const files = hooks.useFiles();
  const worker = hooks.useWorker();
  const workspace = hooks.useWorkspace();
  const updateWorkspace = hooks.useUpdateWorkspaceAction();
  const createFile = hooks.useCreateFileAction();

  const selectorDialogRef = useRef(null);
  const [isSelectorDialogOpen, setIsSelectorDialogOpen] = useState(false);

  const workerPlugins = filter(
    map(worker?.pluginKeys, pluginKey =>
      find(files, file => file.name === pluginKey.name),
    ),
  );

  const workspacePlugins = workspace?.worker?.pluginKeys
    ? filter(
        map(workspace.worker.pluginKeys, pluginKey =>
          find(files, file => file.name === pluginKey.name),
        ),
      )
    : workerPlugins;

  const updatePluginKeysToWorkspace = newPluginKeys => {
    updateWorkspace({
      ...workspace,
      worker: {
        ...workspace?.worker,
        pluginKeys: newPluginKeys,
      },
    });

    const newSelectedFiles = filter(
      map(newPluginKeys, pluginKey =>
        find(files, file => file.name === pluginKey.name),
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
      workspace?.worker?.pluginKeys,
      pluginKey => pluginKey.name === fileToRemove.name,
    );

    if (shouldBeRemoved) {
      const newPluginKeys = reject(
        workspace?.worker?.pluginKeys,
        pluginKey => pluginKey.name === fileToRemove.name,
      );
      updatePluginKeysToWorkspace(newPluginKeys);
    }
  };

  const handleSelectorDialogConfirm = selectedFiles => {
    const newPluginKeys = map(selectedFiles, file => getKey(file));
    updatePluginKeysToWorkspace(newPluginKeys);
    setIsSelectorDialogOpen(false);
  };

  const handleUpload = event => {
    const file = event?.target?.files?.[0];
    if (file) createFile(file);
  };

  const handleUndoIconClick = fileClicked => {
    if (!fileClicked?.group || !fileClicked?.name) return;

    const shouldBeAdded = every(
      workspace?.worker?.pluginKeys,
      pluginKey => pluginKey.name !== fileClicked.name,
    );

    if (shouldBeAdded) {
      const newPluginKeys = [
        ...workspace?.worker?.pluginKeys,
        getKey(fileClicked),
      ];
      updatePluginKeysToWorkspace(newPluginKeys);
    } else {
      handleRemove(fileClicked);
    }
  };

  return (
    <>
      <FileTable
        files={workspacePlugins}
        onRemove={handleRemove}
        options={{
          comparison: true,
          comparedFiles: workerPlugins,
          onAddIconClick: handleAddIconClick,
          onUndoIconClick: handleUndoIconClick,
          showAddIcon: true,
          showUploadIcon: false,
          showDeleteIcon: false,
          showDownloadIcon: false,
          showRemoveIcon: true,
        }}
        title="Plugins"
      />

      <FileSelectorDialog
        isOpen={isSelectorDialogOpen}
        files={files}
        onClose={() => setIsSelectorDialogOpen(false)}
        onConfirm={handleSelectorDialogConfirm}
        onUpload={handleUpload}
        ref={selectorDialogRef}
        selectedFiles={workspacePlugins}
        tableTitle="Workspace files"
      />
    </>
  );
}

export default PluginTable;
