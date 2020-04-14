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

import React, { useEffect, useImperativeHandle, useState } from 'react';
import PropTypes from 'prop-types';
import {
  capitalize,
  flatMap,
  filter,
  find,
  includes,
  isEqual,
  map,
  noop,
  pull,
  round,
  size,
  sortBy,
  sortedUniq,
  uniq,
} from 'lodash';
import LinearProgress from '@material-ui/core/LinearProgress';
import Link from '@material-ui/core/Link';
import IconButton from '@material-ui/core/IconButton';
import Tooltip from '@material-ui/core/Tooltip';
import Typography from '@material-ui/core/Typography';
import VisibilityIcon from '@material-ui/icons/Visibility';
import AddBox from '@material-ui/icons/AddBox';

import { NODE_STATE } from 'api/apiInterface/nodeInterface';
import { KIND, MODE } from 'const';
import * as hooks from 'hooks';
import { Dialog } from 'components/common/Dialog';
import { useConfiguratorState, useViewNodeDialog } from 'context';
import AddNodeDialog from './AddNodeDialog';
import ViewNodeDialog from './ViewNodeDialog';
import { MuiTable } from 'components/common/Table';

const SelectNodeDialog = React.forwardRef((props, ref) => {
  const { initialValues = [], isOpen, onClose, onSubmit, title } = props;
  const fetchNodes = hooks.useFetchNodesAction();
  const nodes = hooks.useAllNodes();
  const isNodeLoaded = hooks.useIsNodeLoaded();

  // selected is an array of hostname, like ['dev01', 'dev02'].
  const [selected, setSelected] = useState(initialValues);
  const [isAddNodeDialogOpen, setIsAddNodeDialogOpen] = useState(false);
  const { open: openViewNodeDialog } = useViewNodeDialog();
  const { data: configuratorInfo } = useConfiguratorState();

  useImperativeHandle(ref, () => ({
    initialize: values => setSelected(values),
  }));

  useEffect(() => {
    if (!isNodeLoaded) fetchNodes();
  }, [fetchNodes, isNodeLoaded]);

  const handleClose = () => {
    setSelected(initialValues);
    onClose();
  };

  const handleSubmit = () => onSubmit(selected);

  const handleAllSelected = selectedRows => {
    setSelected(map(selectedRows, rowData => rowData.hostname));
  };

  const handleSelected = selectedRow => {
    const { hostname } = selectedRow;
    const newSelected = [...selected];
    if (includes(selected, hostname)) {
      pull(newSelected, hostname);
    } else {
      newSelected.push(hostname);
    }
    setSelected(newSelected);
  };

  const unionResourceNames = nodes =>
    uniq(
      flatMap(nodes, node => map(node.resources, resource => resource.name)),
    );

  const getResourceColumns = () => {
    const resourceNames = unionResourceNames(nodes);
    return map(resourceNames, resourceName => ({
      title: resourceName,
      render: rowData => {
        const resource = find(
          rowData.resources,
          resource => resource.name === resourceName,
        );
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

  return (
    <>
      <Dialog
        confirmText="Save"
        handleClose={handleClose}
        handleConfirm={handleSubmit}
        confirmDisabled={isEqual(
          sortedUniq(selected),
          sortedUniq(initialValues),
        )}
        open={isOpen}
        title={title}
        maxWidth="md"
      >
        <MuiTable
          actions={[
            {
              icon: () => <AddBox />,
              tooltip: 'Add Node',
              hidden: configuratorInfo.mode === MODE.K8S,
              isFreeAction: true,
              onClick: () => setIsAddNodeDialogOpen(true),
            },
          ]}
          columns={[
            { title: 'Name', field: 'hostname' },
            ...getResourceColumns(),
            {
              title: 'Services',
              render: rowData => {
                const services = filter(
                  rowData.services,
                  service => service.name !== KIND.configurator,
                );
                const clusters = flatMap(
                  services,
                  service => service.clusterKeys,
                );
                return (
                  <Typography>
                    <Link
                      component="button"
                      href="#"
                      onClick={() => openViewNodeDialog(rowData.hostname)}
                    >
                      {size(clusters)}
                    </Link>
                  </Typography>
                );
              },
            },
            {
              title: 'State',
              render: rowData => capitalize(rowData.state),
            },
            {
              title: 'Actions',
              cellStyle: { textAlign: 'right' },
              headerStyle: { textAlign: 'right' },
              sorting: false,
              render: rowData => (
                <Tooltip title="View Node">
                  <IconButton
                    onClick={() => openViewNodeDialog(rowData.hostname)}
                  >
                    <VisibilityIcon />
                  </IconButton>
                </Tooltip>
              ),
            },
          ]}
          data={sortBy(nodes, node => node.hostname)}
          options={{
            paging: false,
            search: true,
            searchFieldAlignment: 'left',
            selection: true,
            selectionProps: rowData => ({
              checked: includes(selected, rowData.hostname),
              disabled: rowData.state.toUpperCase() === NODE_STATE.UNAVAILABLE,
            }),
            showTitle: false,
            showTextRowsSelected: false,
          }}
          onSelectionChange={(selectRows, dataClicked) =>
            dataClicked
              ? handleSelected(dataClicked)
              : handleAllSelected(selectRows)
          }
        />
      </Dialog>
      <AddNodeDialog
        isOpen={isAddNodeDialogOpen}
        handleClose={() => setIsAddNodeDialogOpen(false)}
        mode={configuratorInfo?.mode || MODE.K8S}
      />
      <ViewNodeDialog mode={configuratorInfo?.mode || MODE.K8S} />
    </>
  );
});
SelectNodeDialog.propTypes = {
  initialValues: PropTypes.arrayOf(PropTypes.string),
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func,
  onSubmit: PropTypes.func,
  title: PropTypes.string,
};

SelectNodeDialog.defaultProps = {
  initialValues: [],
  onClose: noop,
  onSubmit: noop,
  title: 'Nodes',
};

export default SelectNodeDialog;
