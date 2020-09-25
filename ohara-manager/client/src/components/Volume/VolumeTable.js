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

import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { find, isEmpty, isFunction } from 'lodash';

import AddIcon from '@material-ui/icons/Add';
import CreateIcon from '@material-ui/icons/Create';
import RefreshOutlinedIcon from '@material-ui/icons/RefreshOutlined';

import { Actions, MuiTable as Table } from 'components/common/Table';
import VolumeCreateDialog from './VolumeCreateDialog';
import VolumeEditDialog from './VolumeEditDialog';

const defaultOptions = {
  comparison: false,
  comparedNodes: [],
  customColumns: [],
  disabledDeleteIcon: false,
  disabledRemoveIcon: false,
  onCreateIconClick: null,
  onDeleteIconClick: null,
  onDetailIconClick: null,
  onEditorIconClick: null,
  onUndoIconClick: null,
  onRefreshIconClick: null,
  selection: false,
  selectedNodes: [],
  disabledNodes: [],
  showAddIcon: false,
  showCreateIcon: true,
  showDeleteIcon: true,
  showDetailIcon: true,
  showEditorIcon: true,
  showUndoIcon: false,
  showRefreshIcon: false,
  showRemoveIcon: false,
  showTitle: true,
  showServicesColumn: true,
};

function VolumeTable(props) {
  const {
    onCreate,
    onUpdate,
    onSelectionChange,
    title,
    usedBy,
    nodeNames,
    volumes: data,
  } = props;
  const options = { ...defaultOptions, ...props?.options };
  const [activeVolume, setActiveVolume] = useState();
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isEditorDialogOpen, setIsEditorDialogOpen] = useState(false);

  const willBeRemoved = (volume) => !find(data, (v) => v.name === volume.name);

  const willBeAdded = (volume) =>
    !isEmpty(options?.comparedVolumes) &&
    !find(options?.comparedVolumes, (v) => v.name === volume.name);

  const handleAddIconClick = () => {
    if (isFunction(options?.onAddIconClick)) {
      options.onAddIconClick();
    }
  };

  const handleCreateIconClick = () => {
    if (isFunction(options?.onCreateIconClick)) {
      options.onCreateIconClick();
    } else {
      setIsCreateDialogOpen(true);
    }
  };

  const handleEditorIconClick = (volume) => {
    if (isFunction(options?.onEditorIconClick)) {
      options.onEditorIconClick(volume);
    } else {
      setActiveVolume(volume);
      setIsEditorDialogOpen(true);
    }
  };

  const handleUndoIconClick = (node) => {
    if (isFunction(options?.onUndoIconClick)) {
      options.onUndoIconClick(node);
    }
  };

  const handleRefreshIconClick = () => {
    if (isFunction(options?.onRefreshIconClick)) {
      options.onRefreshIconClick();
    }
  };

  const renderRowActions = () => {
    const isShow =
      options?.showDeleteIcon ||
      options?.showDetailIcon ||
      options?.showEditorIcon ||
      options?.showRemoveIcon;

    const render = (volume) => {
      const getUndoTooltipTitle = (volume) => {
        if (willBeAdded(volume)) {
          return 'Undo add volume';
        } else if (willBeRemoved(volume)) {
          return 'Undo remove volume';
        }
        return 'Undo';
      };

      const showUndoIcon = (volume) =>
        (options?.comparison && willBeAdded(volume)) || willBeRemoved(volume);

      return (
        <Actions
          actions={[
            {
              hidden: !options?.showEditorIcon,
              name: 'edit',
              onClick: handleEditorIconClick,
              tooltip: 'Edit volume',
              testid: `edit-volume-${volume.tags.displayName}`,
            },
            {
              hidden: !showUndoIcon(volume),
              name: 'undo',
              onClick: handleUndoIconClick,
              tooltip: getUndoTooltipTitle(volume),
            },
          ]}
          data={volume}
        />
      );
    };

    return {
      cellStyle: { textAlign: 'right' },
      headerStyle: { textAlign: 'right' },
      hidden: !isShow,
      render,
      sorting: false,
      title: 'Actions',
    };
  };

  const getRowStyle = (volume) => {
    if (options?.comparison && willBeRemoved(volume)) {
      return {
        backgroundColor: 'rgba(255, 117, 159, 0.1)',
      };
    } else if (options?.comparison && willBeAdded(volume)) {
      return {
        backgroundColor: 'rgba(114, 204, 255, 0.1)',
      };
    }
    return null;
  };

  return (
    <>
      <Table
        actions={[
          {
            hidden: !options?.showAddIcon,
            icon: () => <AddIcon />,
            isFreeAction: true,
            onClick: handleAddIconClick,
            tooltip: 'Add Volume',
          },
          {
            hidden: !options?.showCreateIcon,
            icon: () => <CreateIcon />,
            isFreeAction: true,
            onClick: handleCreateIconClick,
            tooltip: 'Create Volume',
          },
          {
            hidden: !options?.showRefreshIcon,
            icon: () => <RefreshOutlinedIcon />,
            isFreeAction: true,
            onClick: handleRefreshIconClick,
            tooltip: 'Refresh Volumes',
          },
        ]}
        columns={[
          {
            title: 'Name',
            field: 'tags.displayName',
            type: 'string',
          },
          {
            title: 'Path',
            field: 'path',
            type: 'string',
          },
          {
            title: 'Nodes',
            field: 'nodeNames',
            type: 'array',
          },
          {
            title: 'Used by',
            field: 'tags.usedBy',
            type: 'string',
          },
          renderRowActions(),
        ]}
        data={data}
        onSelectionChange={onSelectionChange}
        options={{
          predicate: 'hostname',
          prompt: options?.prompt,
          rowStyle: (node) => getRowStyle(node),
          selection: options?.selection,
          selectedData: options?.selectedNodes,
          disabledData: options?.disabledNodes,
          showTitle: options?.showTitle,
        }}
        title={title}
      />

      <VolumeCreateDialog
        isOpen={isCreateDialogOpen}
        nodeNames={nodeNames}
        onClose={() => setIsCreateDialogOpen(false)}
        onConfirm={onCreate}
        usedBy={usedBy}
      />
      <VolumeEditDialog
        activeVolume={activeVolume}
        isOpen={isEditorDialogOpen}
        nodeNames={nodeNames}
        onClose={() => setIsEditorDialogOpen(false)}
        onConfirm={onUpdate}
        volume={activeVolume}
      />
    </>
  );
}

VolumeTable.propTypes = {
  volumes: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string,
      path: PropTypes.string,
      nodeNames: PropTypes.string,
      tags: PropTypes.shape({
        displayName: PropTypes.string,
        usedBy: PropTypes.string,
      }),
    }),
  ),
  usedBy: PropTypes.string,
  nodeNames: PropTypes.array,
  onCreate: PropTypes.func,
  onUpdate: PropTypes.func,
  onSelectionChange: PropTypes.func,
  options: PropTypes.shape({
    comparison: PropTypes.bool,
    comparedNodes: PropTypes.array,
    customColumns: PropTypes.arrayOf(
      PropTypes.shape({
        customFilterAndSearch: PropTypes.func,
        field: PropTypes.string,
        render: PropTypes.func,
        title: PropTypes.string,
        type: PropTypes.string,
      }),
    ),
    disabledDeleteIcon: PropTypes.oneOfType([PropTypes.bool, PropTypes.func]),
    disabledRemoveIcon: PropTypes.oneOfType([PropTypes.bool, PropTypes.func]),
    onAddIconClick: PropTypes.func,
    onCreateIconClick: PropTypes.func,
    onDeleteIconClick: PropTypes.func,
    onDetailIconClick: PropTypes.func,
    onEditorIconClick: PropTypes.func,
    onUndoIconClick: PropTypes.func,
    onRefreshIconClick: PropTypes.func,
    prompt: PropTypes.string,
    selection: PropTypes.bool,
    selectedNodes: PropTypes.array,
    disabledNodes: PropTypes.array,
    showAddIcon: PropTypes.bool,
    showCreateIcon: PropTypes.bool,
    showDeleteIcon: PropTypes.bool,
    showDetailIcon: PropTypes.bool,
    showEditorIcon: PropTypes.bool,
    showUndoIcon: PropTypes.bool,
    showRefreshIcon: PropTypes.bool,
    showRemoveIcon: PropTypes.bool,
    showTitle: PropTypes.bool,
    showServicesColumn: PropTypes.bool,
    deleteTooltip: PropTypes.oneOfType([PropTypes.string, PropTypes.func]),
    removeTooltip: PropTypes.oneOfType([PropTypes.string, PropTypes.func]),
  }),
  title: PropTypes.string,
};

VolumeTable.defaultProps = {
  nodes: [],
  onCreate: () => {},
  onDelete: () => {},
  onUpdate: () => {},
  onSelectionChange: () => {},
  options: defaultOptions,
  title: 'Volumes',
};

export default VolumeTable;
