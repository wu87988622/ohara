import React from 'react';
import PropTypes from 'prop-types';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import EditIcon from '@material-ui/icons/Edit';
import IconButton from '@material-ui/core/IconButton';
import { join } from 'lodash';

import { TableLoader } from 'common/Loader';

const styles = theme => ({
  button: {
    margin: theme.spacing.unit,
  },
  input: {
    display: 'none',
  },
});

const headers = ['Host name', 'Services', 'SSH', 'Edit'];

const MuiTable = ({
  nodes,
  isLoading,
  getAllClusterNames,
  getSSHLabel,
  handleEditClick,
}) => {
  if (isLoading) return <TableLoader />;
  return (
    <Table>
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
    </Table>
  );
};

MuiTable.propTypes = {
  isLoading: PropTypes.bool.isRequired,
  getAllClusterNames: PropTypes.func.isRequired,
  handleEditClick: PropTypes.func.isRequired,
  getSSHLabel: PropTypes.func.isRequired,
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

export default MuiTable;
