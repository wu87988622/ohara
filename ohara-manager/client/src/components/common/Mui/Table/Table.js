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
import PropTypes from 'prop-types';
import MuiTable from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import EditIcon from '@material-ui/icons/Edit';
import IconButton from '@material-ui/core/IconButton';
import { join } from 'lodash';

import { TableLoader } from 'components/common/Loader';

const styles = theme => ({
  button: {
    margin: theme.spacing(),
  },
  input: {
    display: 'none',
  },
});

const Table = ({
  nodes,
  isLoading,
  getAllClusterNames,
  getSSHLabel,
  handleEditClick,
  headers,
}) => {
  if (isLoading) return <TableLoader />;
  return (
    <MuiTable>
      <TableHead>
        <TableRow>
          {headers.map(header => {
            return (
              <TableCell align="left" key={header}>
                {header}
              </TableCell>
            );
          })}
        </TableRow>
      </TableHead>
      <TableBody>
        {nodes.map(node => (
          <TableRow key={node.name}>
            <TableCell component="th" scope="row">
              {node.name || ''}
            </TableCell>
            <TableCell align="left">
              {join(getAllClusterNames(node), ', ')}
            </TableCell>
            <TableCell align="left">
              {getSSHLabel(node.user, node.port)}
            </TableCell>
            <TableCell className="has-icon" align="left">
              <IconButton
                color="primary"
                className={styles.button}
                aria-label="Edit"
                data-testid="edit-node-icon"
                onClick={() => handleEditClick(node)}
              >
                <EditIcon />
              </IconButton>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </MuiTable>
  );
};

Table.propTypes = {
  isLoading: PropTypes.bool.isRequired,
  getAllClusterNames: PropTypes.func.isRequired,
  handleEditClick: PropTypes.func.isRequired,
  getSSHLabel: PropTypes.func.isRequired,
  headers: PropTypes.array,
  nodes: PropTypes.arrayOf(
    PropTypes.shape({
      lastModified: PropTypes.number,
      name: PropTypes.string,
      password: PropTypes.string,
      port: PropTypes.number,
      services: PropTypes.array,
      user: PropTypes.string,
    }),
  ),
};

export default Table;
