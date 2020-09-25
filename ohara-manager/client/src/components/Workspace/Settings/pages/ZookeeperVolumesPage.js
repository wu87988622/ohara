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

import { concat, compact, get } from 'lodash';
import { VolumeTable } from 'components/Volume';
import * as hooks from 'hooks';
import { KIND, GROUP } from 'const';
import { serviceName } from 'utils/generate';

function ZookeeperVolumesPage() {
  const volumes = hooks.useVolumesByUsedZookeeper();
  const nodesInZookeeper = hooks.useNodesInZookeeper();
  const updateWorkspace = hooks.useUpdateWorkspaceAction();
  const workspace = hooks.useWorkspace();
  const clearVolumes = hooks.useClearVolumesAction();

  const handleCreate = (volume) => {
    clearVolumes();
    updateWorkspace({
      ...workspace,
      volumes: [
        ...get(workspace, 'volumes', []),
        {
          name: serviceName(),
          path: volume.path,
          nodeNames: volume.nodeNames,
          group: GROUP.VOLUME,
          tags: {
            displayName: volume.displayName,
            usedBy: KIND.zookeeper,
          },
        },
      ],
    });
  };

  const handleUpdate = (volume) => {
    updateWorkspace({
      ...workspace,
      volumes: [
        ...get(workspace, 'volumes', []).filter((v) => v.name !== volume.name),
        {
          name: volume.name,
          path: volume.path,
          nodeNames: volume.nodeNames,
          group: GROUP.VOLUME,
          tags: {
            displayName: volume.displayName,
            usedBy: KIND.zookeeper,
          },
        },
      ],
    });
  };

  const handleUndoIconClick = (volume) => {
    updateWorkspace({
      ...workspace,
      volumes: [
        ...get(workspace, 'volumes', []).filter((v) => v.name !== volume.name),
      ],
    });
  };

  const mergeVolumes = () => {
    const tmpVolume = compact(workspace?.volumes);
    return concat(volumes, tmpVolume);
  };

  return (
    <VolumeTable
      nodeNames={nodesInZookeeper}
      onCreate={handleCreate}
      onUpdate={handleUpdate}
      options={{
        showCreateIcon: false,
        showDeleteIcon: false,
        onUndoIconClick: handleUndoIconClick,
        comparedVolumes: volumes,
        comparison: true,
      }}
      title="Volumes"
      usedBy={KIND.zookeeper}
      volumes={mergeVolumes()}
    />
  );
}

export default ZookeeperVolumesPage;
