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
import styled from 'styled-components';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';

import { TableLoader } from 'components/common/Mui/Loader';

const StyledTableCell = styled(TableCell)`
  padding: ${props => props.theme.spacing(1)}px;
  border-bottom: none;
`;

const MuiTable = props => {
  const { headers, isLoading = false, children } = props;
  const lastIdx = headers.length - 1; // Make sure we have the same length as idx

  if (isLoading) return <TableLoader />;

  return (
    <Table>
      <TableHead>
        <TableRow>
          {headers.map((header, idx) => {
            // The last table cell should be aligned to right
            const align = idx === lastIdx ? 'right' : 'left';
            return (
              <StyledTableCell align={align} key={header}>
                {header}
              </StyledTableCell>
            );
          })}
        </TableRow>
      </TableHead>
      <TableBody>{children()}</TableBody>
    </Table>
  );
};

MuiTable.propTypes = {
  headers: PropTypes.arrayOf(PropTypes.string).isRequired,
  children: PropTypes.func.isRequired,
  isLoading: PropTypes.bool,
};

export default MuiTable;
