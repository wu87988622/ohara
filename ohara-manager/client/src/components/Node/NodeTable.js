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
  map,
  round,
  some,
  reject,
  size,
  uniq,
} from 'lodash';
import Checkbox from '@material-ui/core/Checkbox';
import LinearProgress from '@material-ui/core/LinearProgress';
import Link from '@material-ui/core/Link';
import IconButton from '@material-ui/core/IconButton';
import Tooltip from '@material-ui/core/Tooltip';
import AddBox from '@material-ui/icons/AddBox';
import Typography from '@material-ui/core/Typography';
import EditIcon from '@material-ui/icons/Edit';
import DeleteIcon from '@material-ui/icons/Delete';
import VisibilityIcon from '@material-ui/icons/Visibility';

import Table from 'components/common/Table/MuiTable';
import { KIND } from 'const';

const getUnionResourceNames = nodes =>
  uniq(flatMap(nodes, node => map(node.resources, resource => resource.name)));

function NodeTable(props) {
  const {
    nodes,
    onCreateClick,
    onDeleteClick,
    onDetailClick,
    onEditorClick,
    onSelectionChange,
    title,
    selection,
    selectedNodes,
    showCreateIcon,
    showDeleteIcon,
    showDetailIcon,
    showEditorIcon,
  } = props;

  const [selected, setSelected] = useState(selectedNodes);

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

  const renderActions = () => {
    return {
      title: 'Actions',
      cellStyle: { textAlign: 'right' },
      headerStyle: { textAlign: 'right' },
      sorting: false,
      render: node => (
        <>
          {showDetailIcon && (
            <Tooltip title="View node">
              <IconButton
                data-testid={`view-node-${node.hostname}`}
                onClick={() => {
                  onDetailClick(node);
                }}
              >
                <VisibilityIcon />
              </IconButton>
            </Tooltip>
          )}
          {showEditorIcon && (
            <Tooltip title="Edit node">
              <IconButton
                onClick={() => {
                  onEditorClick(node);
                }}
              >
                <EditIcon />
              </IconButton>
            </Tooltip>
          )}
          {showDeleteIcon && (
            <Tooltip title="Delete node">
              <IconButton
                onClick={() => {
                  onDeleteClick(node);
                }}
              >
                <DeleteIcon />
              </IconButton>
            </Tooltip>
          )}
        </>
      ),
    };
  };

  const renderResourceColumns = nodes => {
    const unionResourceNames = getUnionResourceNames(nodes);
    return map(unionResourceNames, resourceName => ({
      title: resourceName,
      render: node => {
        const resource = find(node.resources, r => r.name === resourceName);
        if (!resource) return;
        const { unit, used, value } = resource;
        return (
          <>
            <Typography variant="subtitle2">
              {value} {unit}
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
                onDetailClick(node);
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

  const renderColumns = [
    { title: 'Name', field: 'hostname' },
    ...renderResourceColumns(nodes),
    renderServiceColumn(),
    {
      title: 'State',
      field: 'state',
      render: node => capitalize(node.state),
    },
  ];

  if (selection) {
    renderColumns.unshift(renderSelectionColumn());
  }

  if (showDeleteIcon || showDetailIcon || showEditorIcon) {
    renderColumns.push(renderActions());
  }

  return (
    <Table
      title={title}
      actions={[
        {
          icon: () => <AddBox />,
          tooltip: 'Add Node',
          hidden: !showCreateIcon,
          isFreeAction: true,
          onClick: onCreateClick,
        },
      ]}
      columns={renderColumns}
      data={nodes}
      options={{
        paging: false,
        search: true,
      }}
    />
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
  onCreateClick: PropTypes.func,
  onDeleteClick: PropTypes.func,
  onDetailClick: PropTypes.func,
  onEditorClick: PropTypes.func,
  onSelectionChange: PropTypes.func,
  title: PropTypes.string,
  selection: PropTypes.bool,
  selectedNodes: PropTypes.array,
  showCreateIcon: PropTypes.bool,
  showDeleteIcon: PropTypes.bool,
  showDetailIcon: PropTypes.bool,
  showEditorIcon: PropTypes.bool,
};

NodeTable.defaultProps = {
  nodes: [],
  onCreateClick: () => {},
  onDeleteClick: () => {},
  onDetailClick: () => {},
  onEditorClick: () => {},
  onSelectionChange: () => {},
  title: 'Nodes',
  selection: false,
  selectedNodes: [],
  showCreateIcon: true,
  showDeleteIcon: true,
  showDetailIcon: true,
  showEditorIcon: true,
};

export default NodeTable;
