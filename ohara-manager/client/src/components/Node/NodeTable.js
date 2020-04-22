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
import {
  capitalize,
  flatMap,
  filter,
  find,
  isEmpty,
  isFunction,
  map,
  reject,
  round,
  some,
  sortBy,
  size,
  unionBy,
  uniq,
} from 'lodash';
import Checkbox from '@material-ui/core/Checkbox';
import LinearProgress from '@material-ui/core/LinearProgress';
import Link from '@material-ui/core/Link';
import IconButton from '@material-ui/core/IconButton';
import Tooltip from '@material-ui/core/Tooltip';
import Typography from '@material-ui/core/Typography';

import AddBoxIcon from '@material-ui/icons/AddBox';
import AddIcon from '@material-ui/icons/Add';
import ClearIcon from '@material-ui/icons/Clear';
import DeleteIcon from '@material-ui/icons/Delete';
import EditIcon from '@material-ui/icons/Edit';
import SettingsBackupRestoreIcon from '@material-ui/icons/SettingsBackupRestore';
import VisibilityIcon from '@material-ui/icons/Visibility';

import Table from 'components/common/Table/MuiTable';
import { KIND, MODE } from 'const';
import NodeCreateDialog from './NodeCreateDialog';
import NodeDeleteDialog from './NodeDeleteDialog';
import NodeDetailDialog from './NodeDetailDialog';
import NodeEditorDialog from './NodeEditorDialog';
import NodeRemoveDialog from './NodeRemoveDialog';

const defaultOptions = {
  comparison: false,
  comparedNodes: [],
  mode: MODE.K8S,
  onCreateIconClick: null,
  onDeleteIconClick: null,
  onDetailIconClick: null,
  onEditorIconClick: null,
  onUndoIconClick: null,
  onRemoveIconClick: null,
  selection: false,
  selectedNodes: [],
  showAddIcon: false,
  showCreateIcon: true,
  showDeleteIcon: true,
  showDetailIcon: true,
  showEditorIcon: true,
  showUndoIcon: false,
  showRemoveIcon: false,
  showTitle: true,
};

const getUnionResourceNames = nodes =>
  uniq(flatMap(nodes, node => map(node.resources, resource => resource.name)));

function NodeTable(props) {
  const {
    nodes,
    onCreate,
    onDelete,
    onUpdate,
    onRemove,
    onSelectionChange,
    title,
  } = props;

  const options = { ...defaultOptions, ...props?.options };

  const [activeNode, setActiveNode] = useState();
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isDetailDialogOpen, setIsDetailDialogOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [isEditorDialogOpen, setIsEditorDialogOpen] = useState(false);
  const [isRemoveDialogOpen, setIsRemoveDialogOpen] = useState(false);
  const [selected, setSelected] = useState(options?.selectedNodes || []);

  const willBeRemoved = node => !find(nodes, n => n.hostname === node.hostname);

  const willBeAdded = node =>
    !isEmpty(options?.comparedNodes) &&
    !find(options?.comparedNodes, n => n.hostname === node.hostname);

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

  const handleDeleteIconClick = node => {
    if (isFunction(options?.onDeleteIconClick)) {
      options.onDeleteIconClick(node);
    } else {
      setIsDeleteDialogOpen(true);
      setActiveNode(node);
    }
  };

  const handleDetailIconClick = node => {
    if (isFunction(options?.onDetailIconClick)) {
      options.onDetailIconClick(node);
    } else {
      setIsDetailDialogOpen(true);
      setActiveNode(node);
    }
  };

  const handleEditorIconClick = node => {
    if (isFunction(options?.onEditorIconClick)) {
      options.onEditorIconClick(node);
    } else {
      setIsEditorDialogOpen(true);
      setActiveNode(node);
    }
  };

  const handleUndoIconClick = node => {
    if (isFunction(options?.onUndoIconClick)) {
      options.onUndoIconClick(node);
    }
  };

  const handleRemoveIconClick = node => {
    if (isFunction(options?.onRemoveIconClick)) {
      options.onRemoveIconClick(node);
    } else {
      setIsRemoveDialogOpen(true);
      setActiveNode(node);
    }
  };

  const handleCreateDialogConfirm = nodeToCreate => {
    onCreate(nodeToCreate);
    setIsCreateDialogOpen(false);
  };

  const handleDeleteDialogConfirm = nodeToDelete => {
    onDelete(nodeToDelete);
    setIsDeleteDialogOpen(false);
  };

  const handleEditorDialogConfirm = nodeToUpdate => {
    onUpdate(nodeToUpdate);
    setIsEditorDialogOpen(false);
  };

  const handleRemoveDialogConfirm = nodeToRemove => {
    onRemove(nodeToRemove);
    setIsRemoveDialogOpen(false);
  };

  const handleRowSelected = (event, dataClicked) => {
    if (dataClicked) {
      const newSelected = some(
        selected,
        n => n.hostname === dataClicked.hostname,
      )
        ? reject(selected, n => n.hostname === dataClicked.hostname)
        : [...selected, dataClicked];

      setSelected(newSelected);
      onSelectionChange(newSelected, dataClicked);
    }
    event.stopPropagation();
  };

  const renderRowActions = () => {
    return {
      title: 'Actions',
      cellStyle: { textAlign: 'right' },
      headerStyle: { textAlign: 'right' },
      sorting: false,
      render: node => {
        const getUndoTooltipTitle = node => {
          if (willBeAdded(node)) {
            return 'Undo add node';
          } else if (willBeRemoved(node)) {
            return 'Undo remove node';
          }
          return 'Undo';
        };

        const showUndoIcon = node =>
          (options?.comparison && willBeAdded(node)) || willBeRemoved(node);

        const showRemoveIcon = node =>
          options?.showRemoveIcon && !showUndoIcon(node);

        return (
          <>
            {options?.showDetailIcon && (
              <Tooltip title="View node">
                <IconButton
                  data-testid={`view-node-${node.hostname}`}
                  onClick={() => {
                    handleDetailIconClick(node);
                  }}
                >
                  <VisibilityIcon />
                </IconButton>
              </Tooltip>
            )}
            {options?.showEditorIcon && (
              <Tooltip title="Edit node">
                <IconButton
                  onClick={() => {
                    handleEditorIconClick(node);
                  }}
                >
                  <EditIcon />
                </IconButton>
              </Tooltip>
            )}
            {options?.showDeleteIcon && (
              <Tooltip title="Delete node">
                <IconButton
                  onClick={() => {
                    handleDeleteIconClick(node);
                  }}
                >
                  <DeleteIcon />
                </IconButton>
              </Tooltip>
            )}
            {showRemoveIcon(node) && (
              <Tooltip title="Remove node">
                <IconButton
                  onClick={() => {
                    handleRemoveIconClick(node);
                  }}
                >
                  <ClearIcon />
                </IconButton>
              </Tooltip>
            )}
            {showUndoIcon(node) && (
              <Tooltip title={getUndoTooltipTitle(node)}>
                <IconButton
                  onClick={() => {
                    handleUndoIconClick(node);
                  }}
                >
                  <SettingsBackupRestoreIcon />
                </IconButton>
              </Tooltip>
            )}
          </>
        );
      },
    };
  };

  const renderResourceColumns = () => {
    const unionResourceNames = getUnionResourceNames(getData());
    return map(unionResourceNames, resourceName => ({
      title: resourceName,
      render: node => {
        const resource = find(node.resources, r => r.name === resourceName);
        if (!resource) return;
        const { unit, used, value } = resource;
        return (
          <>
            <Typography variant="subtitle2">
              {round(value, 1)} {unit}
            </Typography>
            <Tooltip title={`${round(used * 100, 1)} %`}>
              <LinearProgress
                value={used * 100}
                variant="determinate"
                color={used > 0.8 ? 'secondary' : 'primary'}
              />
            </Tooltip>
          </>
        );
      },
    }));
  };

  const renderSelectionColumn = () => {
    const style = { paddingLeft: '0px', paddingRight: '0px', width: '42px' };
    return {
      cellStyle: style,
      headerStyle: style,
      render: node => (
        <Checkbox
          checked={some(selected, n => n.hostname === node.hostname)}
          color="primary"
          onChange={event => handleRowSelected(event, node)}
        />
      ),
    };
  };

  const renderServiceColumn = () => {
    return {
      title: 'Services',
      render: node => {
        const services = filter(
          node.services,
          service => service.name !== KIND.configurator,
        );
        const clusters = flatMap(services, service => service.clusterKeys);
        return (
          <Typography>
            <Link
              component="button"
              variant="h6"
              onClick={event => {
                handleDetailIconClick(node);
                event.stopPropagation();
              }}
            >
              {size(clusters)}
            </Link>
          </Typography>
        );
      },
    };
  };

  const renderActions = () => {
    return [
      {
        icon: () => <AddIcon />,
        tooltip: 'Add Node',
        hidden: !options?.showAddIcon,
        isFreeAction: true,
        onClick: handleAddIconClick,
      },
      {
        icon: () => <AddBoxIcon />,
        tooltip: 'Create Node',
        hidden: !options?.showCreateIcon,
        isFreeAction: true,
        onClick: handleCreateIconClick,
      },
    ];
  };

  const renderColumns = () => {
    const columns = [
      { title: 'Name', field: 'hostname' },
      ...renderResourceColumns(),
      renderServiceColumn(),
      {
        title: 'State',
        field: 'state',
        render: node => capitalize(node.state),
      },
    ];

    if (options?.selection) {
      columns.unshift(renderSelectionColumn());
    }

    if (
      options?.showDeleteIcon ||
      options?.showDetailIcon ||
      options?.showEditorIcon ||
      options?.showRemoveIcon
    ) {
      columns.push(renderRowActions());
    }

    return columns;
  };

  const getData = () => {
    if (options?.comparison) {
      return sortBy(unionBy(options?.comparedNodes, nodes, 'hostname'), [
        'hostname',
      ]);
    }
    return nodes;
  };

  const getRowStyle = node => {
    if (options?.comparison && willBeRemoved(node)) {
      return {
        backgroundColor: 'rgba(255, 117, 159, 0.1)',
      };
    } else if (options?.comparison && willBeAdded(node)) {
      return {
        backgroundColor: 'rgba(114, 204, 255, 0.1)',
      };
    }
    return null;
  };

  return (
    <>
      <Table
        title={title}
        actions={renderActions()}
        columns={renderColumns()}
        data={getData()}
        options={{
          paging: false,
          search: true,
          showTitle: options?.showTitle,
          rowStyle: node => getRowStyle(node),
        }}
      />
      <NodeCreateDialog
        isOpen={isCreateDialogOpen}
        mode={options?.mode}
        onClose={() => setIsCreateDialogOpen(false)}
        onConfirm={handleCreateDialogConfirm}
      />
      <NodeDeleteDialog
        isOpen={isDeleteDialogOpen}
        node={activeNode}
        onClose={() => setIsDeleteDialogOpen(false)}
        onConfirm={handleDeleteDialogConfirm}
      />
      <NodeDetailDialog
        isOpen={isDetailDialogOpen}
        mode={options?.mode}
        node={activeNode}
        onClose={() => setIsDetailDialogOpen(false)}
      />
      <NodeEditorDialog
        isOpen={isEditorDialogOpen}
        mode={options?.mode}
        node={activeNode}
        onClose={() => setIsEditorDialogOpen(false)}
        onConfirm={handleEditorDialogConfirm}
      />
      <NodeRemoveDialog
        isOpen={isRemoveDialogOpen}
        node={activeNode}
        onClose={() => setIsRemoveDialogOpen(false)}
        onConfirm={handleRemoveDialogConfirm}
      />
    </>
  );
}

NodeTable.propTypes = {
  nodes: PropTypes.arrayOf(
    PropTypes.shape({
      hostname: PropTypes.string,
      state: PropTypes.string,
      resources: PropTypes.arrayOf(
        PropTypes.shape({
          name: PropTypes.string,
          unit: PropTypes.string,
          used: PropTypes.number,
          value: PropTypes.number,
        }),
      ),
      services: PropTypes.arrayOf(
        PropTypes.shape({
          name: PropTypes.string,
          clusterKeys: PropTypes.array,
        }),
      ),
    }),
  ),
  onCreate: PropTypes.func,
  onDelete: PropTypes.func,
  onUpdate: PropTypes.func,
  onRemove: PropTypes.func,
  onSelectionChange: PropTypes.func,
  options: PropTypes.shape({
    comparison: PropTypes.bool,
    comparedNodes: PropTypes.array,
    mode: PropTypes.string,
    onAddIconClick: PropTypes.func,
    onCreateIconClick: PropTypes.func,
    onDeleteIconClick: PropTypes.func,
    onDetailIconClick: PropTypes.func,
    onEditorIconClick: PropTypes.func,
    onUndoIconClick: PropTypes.func,
    onRemoveIconClick: PropTypes.func,
    selection: PropTypes.bool,
    selectedNodes: PropTypes.array,
    showAddIcon: PropTypes.bool,
    showCreateIcon: PropTypes.bool,
    showDeleteIcon: PropTypes.bool,
    showDetailIcon: PropTypes.bool,
    showEditorIcon: PropTypes.bool,
    showUndoIcon: PropTypes.bool,
    showRemoveIcon: PropTypes.bool,
    showTitle: PropTypes.bool,
  }),
  title: PropTypes.string,
};

NodeTable.defaultProps = {
  nodes: [],
  onCreate: () => {},
  onDelete: () => {},
  onUpdate: () => {},
  onRemove: () => {},
  onSelectionChange: () => {},
  options: defaultOptions,
  title: 'Nodes',
};

export default NodeTable;
