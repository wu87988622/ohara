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

import React, { useMemo, useState, useRef } from 'react';
import { every, filter, find, map, some, reject } from 'lodash';

import { FileTable } from 'components/File';
import * as hooks from 'hooks';
import { getKey } from 'utils/object';
import WorkspaceFileSelectorDialog from '../common/WorkspaceFileSelectorDialog';

function StreamJarsPage() {
  const workspaceFiles = hooks.useFiles();
  const workspace = hooks.useWorkspace();
  const updateWorkspace = hooks.useUpdateWorkspaceAction();

  const selectorDialogRef = useRef(null);
  const [isSelectorDialogOpen, setIsSelectorDialogOpen] = useState(false);

  const streamJars = useMemo(() => {
    return filter(
      map(workspace?.stream?.jarKeys, jarKey =>
        find(workspaceFiles, file => file.name === jarKey.name),
      ),
    );
  }, [workspace, workspaceFiles]);

  const updateStreamJarKeysToWorkspace = newJarKeys => {
    updateWorkspace({
      ...workspace,
      stream: {
        ...workspace?.stream,
        jarKeys: newJarKeys,
      },
    });

    const newSelectedFiles = filter(
      map(newJarKeys, jarKey =>
        find(workspaceFiles, file => file.name === jarKey.name),
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
      workspace?.stream?.jarKeys,
      jarKey => jarKey.name === fileToRemove.name,
    );

    if (shouldBeRemoved) {
      const newJarKeys = reject(
        workspace?.stream?.jarKeys,
        jarKey => jarKey.name === fileToRemove.name,
      );
      updateStreamJarKeysToWorkspace(newJarKeys);
    }
  };

  const handleSelectorDialogConfirm = selectedFiles => {
    const newJarKeys = map(selectedFiles, file => getKey(file));
    updateStreamJarKeysToWorkspace(newJarKeys);
    setIsSelectorDialogOpen(false);
  };

  const handleUndoIconClick = fileClicked => {
    if (!fileClicked?.group || !fileClicked?.name) return;

    const shouldBeAdded = every(
      workspace?.stream?.jarKeys,
      jarKey => jarKey.name !== fileClicked.name,
    );

    if (shouldBeAdded) {
      const newJarKeys = [workspace?.stream?.jarKeys, getKey(fileClicked)];
      updateStreamJarKeysToWorkspace([newJarKeys]);
    } else {
      handleRemove(fileClicked);
    }
  };

  return (
    <>
      <FileTable
        files={streamJars}
        onRemove={handleRemove}
        options={{
          onAddIconClick: handleAddIconClick,
          onUndoIconClick: handleUndoIconClick,
          showAddIcon: true,
          showUploadIcon: false,
          showDeleteIcon: false,
          showDownloadIcon: false,
          showRemoveIcon: true,
        }}
        title="Stream jars"
      />

      <WorkspaceFileSelectorDialog
        isOpen={isSelectorDialogOpen}
        onClose={() => setIsSelectorDialogOpen(false)}
        onConfirm={handleSelectorDialogConfirm}
        ref={selectorDialogRef}
        tableProps={{
          options: {
            selectedFiles: streamJars,
            showDeleteIcon: true,
          },
          title: 'Files',
        }}
      />
    </>
  );
}

export default StreamJarsPage;
