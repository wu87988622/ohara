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
import {
  every,
  filter,
  find,
  includes,
  isEmpty,
  map,
  reject,
  some,
  toUpper,
} from 'lodash';
import Link from '@material-ui/core/Link';
import Tooltip from '@material-ui/core/Tooltip';

import { FileTable, FileRemoveDialog } from 'components/File';
import * as context from 'context';
import * as hooks from 'hooks';
import { getKey } from 'utils/object';
import WorkspaceFileSelectorDialog from '../common/WorkspaceFileSelectorDialog';

function StreamJarsPage() {
  const workspaceFiles = hooks.useFiles();
  const workspace = hooks.useWorkspace();
  const updateWorkspace = hooks.useUpdateWorkspaceAction();
  const pipelines = hooks.usePipelines();
  const switchPipeline = hooks.useSwitchPipelineAction();
  const { close: closeSettingsDialog } = context.useEditWorkspaceDialog();

  const selectorDialogRef = useRef(null);
  const [isSelectorDialogOpen, setIsSelectorDialogOpen] = useState(false);
  const [isRemoveDialogOpen, setIsRemoveDialogOpen] = useState(false);
  const [activeFile, setActiveFile] = useState();

  const streamJars = useMemo(() => {
    return filter(
      map(workspace?.stream?.jarKeys, jarKey =>
        find(workspaceFiles, file => file.name === jarKey.name),
      ),
    ).map(file => {
      const classNames = file?.classInfos
        ?.filter(classInfo => classInfo?.classType === 'stream')
        ?.map(classInfo => classInfo?.className);

      return {
        ...file,
        pipelines: filter(pipelines, pipeline => {
          return some(pipeline?.objects, object =>
            includes(classNames, object?.className),
          );
        }),
      };
    });
  }, [workspace, workspaceFiles, pipelines]);

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

  const handleRemoveIconClick = fileClicked => {
    setActiveFile(fileClicked);
    setIsRemoveDialogOpen(true);
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

  const handleLinkClick = pipelineClicked => {
    if (pipelineClicked?.name) {
      closeSettingsDialog();
      switchPipeline(pipelineClicked.name);
    }
  };

  return (
    <>
      <FileTable
        files={streamJars}
        options={{
          onAddIconClick: handleAddIconClick,
          onRemoveIconClick: handleRemoveIconClick,
          onUndoIconClick: handleUndoIconClick,
          showAddIcon: true,
          showUploadIcon: false,
          showDeleteIcon: false,
          showDownloadIcon: false,
          showRemoveIcon: true,
          customColumns: [
            {
              title: 'Pipelines',
              customFilterAndSearch: (filterValue, file) => {
                const value = file?.pipelines
                  ?.map(pipeline => pipeline?.name)
                  .join();
                return includes(toUpper(value), toUpper(filterValue));
              },
              render: file => {
                return (
                  <>
                    {map(file?.pipelines, pipeline => (
                      <div key={pipeline.name}>
                        <Tooltip title="Click the link to switch to that pipeline">
                          <Link
                            component="button"
                            onClick={() => handleLinkClick(pipeline)}
                            variant="body2"
                          >
                            {pipeline.name}
                          </Link>
                        </Tooltip>
                      </div>
                    ))}
                  </>
                );
              },
            },
          ],
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

      <FileRemoveDialog
        file={activeFile}
        isOpen={isRemoveDialogOpen}
        onClose={() => setIsRemoveDialogOpen(false)}
        onConfirm={file => {
          handleRemove(file);
          setIsRemoveDialogOpen(false);
        }}
        options={{
          content: file => {
            if (!isEmpty(file?.pipelines)) {
              return `The file ${file?.name} is already used by pipeline ${file?.pipelines?.[0]?.name}. It is not recommended that you remove this file because it will cause the pipeline to fail to start properly.`;
            } else {
              return `Are you sure you want to remove the file ${file?.name}?`;
            }
          },
          confirmDisabled: file => !isEmpty(file?.pipelines),
          showForceCheckbox: file => !isEmpty(file?.pipelines),
        }}
      />
    </>
  );
}

export default StreamJarsPage;
