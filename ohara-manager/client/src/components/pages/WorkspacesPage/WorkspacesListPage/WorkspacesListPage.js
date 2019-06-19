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
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';
import IconButton from '@material-ui/core/IconButton';

import { Table } from 'components/common/Mui/Table';
import { workersPropType } from 'propTypes/services';
import { StyledIcon } from './styles';

const WorkspacesListPage = props => {
  const { workers, isLoading } = props;
  const headers = ['Name', 'Nodes', 'Action'];

  const handleRedirect = workspaceName => {
    props.history.push(`/workspaces/${workspaceName}/overview`);
  };

  return (
    <Table headers={headers} isLoading={isLoading}>
      {() => {
        return workers.map(d => {
          const { name: workspaceName, nodeNames } = d;
          return (
            <TableRow key={workspaceName}>
              <TableCell scope="row">{workspaceName}</TableCell>
              <TableCell align="left">{nodeNames.join(',')}</TableCell>
              <TableCell align="left">
                <IconButton
                  color="primary"
                  data-testid="edit-node-icon"
                  onClick={() => handleRedirect(workspaceName)}
                >
                  <StyledIcon className="fas fa-external-link-square-alt" />
                </IconButton>
              </TableCell>
            </TableRow>
          );
        });
      }}
    </Table>
  );
};

WorkspacesListPage.propTypes = {
  workers: PropTypes.arrayOf(workersPropType).isRequired,
  isLoading: PropTypes.bool,
  history: PropTypes.shape({
    push: PropTypes.func.isRequired,
  }).isRequired,
};

export default WorkspacesListPage;
