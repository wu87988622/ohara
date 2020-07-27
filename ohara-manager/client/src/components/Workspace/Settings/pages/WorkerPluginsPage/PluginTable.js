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
import * as hooks from 'hooks';
import { getKey } from 'utils/object';
import WorkspaceFileSelectorDialog from '../../common/WorkspaceFileSelectorDialog';

function PluginTable() {
  const workspaceFiles = hooks.useFiles();
  const worker = hooks.useWorker();
  const workspace = hooks.useWorkspace();
  const updateWorkspace = hooks.useUpdateWorkspaceAction();
  const pipelines = hooks.usePipelines();
  const switchPipeline = hooks.useSwitchPipelineAction();
  const closeSettings = hooks.useCloseSettingsAction();

  const selectorDialogRef = useRef(null);
  const [isSelectorDialogOpen, setIsSelectorDialogOpen] = useState(false);
  const [isRemoveDialogOpen, setIsRemoveDialogOpen] = useState(false);
  const [activeFile, setActiveFile] = useState();

  const documentation = useMemo(
    () =>
      find(
        worker?.settingDefinitions,
        (definition) => definition.key === 'pluginKeys',
      )?.documentation,
    [worker],
  );

  const workerPlugins = useMemo(() => {
    return filter(
      map(worker?.pluginKeys, (pluginKey) =>
        find(workspaceFiles, (file) => file.name === pluginKey.name),
      ),
    ).map((file) => {
      const classNames = file?.classInfos
        ?.filter(
          (classInfo) =>
            classInfo?.classType === 'source' ||
            classInfo?.classType === 'sink',
        )
        ?.map((classInfo) => classInfo?.className);

      return {
        ...file,
        pipelines: filter(pipelines, (pipeline) => {
          return some(pipeline?.objects, (object) =>
            includes(classNames, object?.className),
          );
        }),
      };
    });
  }, [worker, workspaceFiles, pipelines]);

  const workspacePlugins = useMemo(() => {
    return workspace?.worker?.pluginKeys
      ? filter(
          map(workspace.worker.pluginKeys, (pluginKey) =>
            find(workspaceFiles, (file) => file.name === pluginKey.name),
          ),
        )
      : workerPlugins;
  }, [workspace, workerPlugins, workspaceFiles]);

  const updatePluginKeysToWorkspace = (newPluginKeys) => {
    updateWorkspace({
      ...workspace,
      worker: {
        ...workspace?.worker,
        pluginKeys: newPluginKeys,
      },
    });

    const newSelectedFiles = filter(
      map(newPluginKeys, (pluginKey) =>
        find(workspaceFiles, (file) => file.name === pluginKey.name),
      ),
    );

    selectorDialogRef.current.setSelectedFiles(newSelectedFiles);
  };

  const handleAddIconClick = () => {
    setIsSelectorDialogOpen(true);
  };

  const handleRemoveIconClick = (fileClicked) => {
    setActiveFile(fileClicked);
    setIsRemoveDialogOpen(true);
  };

  const handleRemove = (fileToRemove) => {
    if (!fileToRemove?.group || !fileToRemove?.name) return;

    const shouldBeRemoved = some(
      workspace?.worker?.pluginKeys,
      (pluginKey) => pluginKey.name === fileToRemove.name,
    );

    if (shouldBeRemoved) {
      const newPluginKeys = reject(
        workspace?.worker?.pluginKeys,
        (pluginKey) => pluginKey.name === fileToRemove.name,
      );
      updatePluginKeysToWorkspace(newPluginKeys);
    }
  };

  const handleSelectorDialogConfirm = (selectedFiles) => {
    const newPluginKeys = map(selectedFiles, (file) => getKey(file));
    updatePluginKeysToWorkspace(newPluginKeys);
    setIsSelectorDialogOpen(false);
  };

  const handleUndoIconClick = (fileClicked) => {
    if (!fileClicked?.group || !fileClicked?.name) return;

    const shouldBeAdded = every(
      workspace?.worker?.pluginKeys,
      (pluginKey) => pluginKey.name !== fileClicked.name,
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

  const handleLinkClick = (pipelineClicked) => {
    if (pipelineClicked?.name) {
      closeSettings();
      switchPipeline(pipelineClicked.name);
    }
  };

  return (
    <>
      <FileTable
        files={workspacePlugins}
        options={{
          comparison: true,
          comparedFiles: workerPlugins,
          onAddIconClick: handleAddIconClick,
          onRemoveIconClick: handleRemoveIconClick,
          onUndoIconClick: handleUndoIconClick,
          prompt: documentation,
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
                  ?.map((pipeline) => pipeline?.name)
                  .join();
                return includes(toUpper(value), toUpper(filterValue));
              },
              render: (file) => {
                return (
                  <>
                    {map(file?.pipelines, (pipeline) => (
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
        title="Plugins"
      />

      <WorkspaceFileSelectorDialog
        isOpen={isSelectorDialogOpen}
        onClose={() => setIsSelectorDialogOpen(false)}
        onConfirm={handleSelectorDialogConfirm}
        ref={selectorDialogRef}
        tableProps={{
          options: {
            selectedFiles: workspacePlugins,
          },
          title: 'Files',
        }}
      />

      <FileRemoveDialog
        file={activeFile}
        isOpen={isRemoveDialogOpen}
        onClose={() => setIsRemoveDialogOpen(false)}
        onConfirm={(file) => {
          handleRemove(file);
          setIsRemoveDialogOpen(false);
        }}
        options={{
          content: (file) => {
            if (!isEmpty(file?.pipelines)) {
              return `The file ${file?.name} is already used by pipeline ${file?.pipelines?.[0]?.name}. It is not recommended that you remove this file because it will cause the pipeline to fail to start properly.`;
            } else {
              return `Are you sure you want to remove the file ${file?.name}?`;
            }
          },
          confirmDisabled: (file) => !isEmpty(file?.pipelines),
          showForceCheckbox: (file) => !isEmpty(file?.pipelines),
        }}
      />
    </>
  );
}

export default PluginTable;
