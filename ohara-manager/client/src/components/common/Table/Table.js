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
import TableFooter from '@material-ui/core/TableFooter';
import Paper from '@material-ui/core/Paper';
import Typography from '@material-ui/core/Typography';

import { TableLoader } from 'components/common/Loader';

const Wrapper = styled.div`
  border: 1px solid ${props => props.theme.palette.grey[300]};
  border-radius: ${props => props.theme.shape.borderRadius}px;
  overflow: hidden;

  &.has-title {
    h6 {
      padding: ${props => props.theme.spacing(2)}px;
      border-bottom: 1px solid ${props => props.theme.palette.grey[300]};
    }
  }
`;

const StyledTableHead = styled(TableHead)`
  background-color: ${props => props.theme.palette.grey[100]};
`;

const StyledFooter = styled(TableFooter)`
  background-color: ${props => props.theme.palette.grey[50]};
`;

const MuiTable = props => {
  const { headers, title = '', isLoading = false, children, footer } = props;
  const lastIdx = headers.length - 1; // Make sure we have the same length as idx

  const hasTitle = title.length > 0 ? true : false;
  const titleClass = hasTitle ? 'has-title' : '';

  if (isLoading) return <TableLoader />;

  return (
    <Wrapper className={`${titleClass}`}>
      <Paper elevation={1}>
        {hasTitle && <Typography variant="h6">{title}</Typography>}
        <Table>
          <StyledTableHead>
            <TableRow>
              {headers.map((header, idx) => {
                // The last table cell should be aligned to right
                const align = idx === lastIdx ? 'right' : 'left';
                return (
                  <TableCell align={align} key={header}>
                    {header}
                  </TableCell>
                );
              })}
            </TableRow>
          </StyledTableHead>
          <TableBody>{children}</TableBody>
          {footer ? <StyledFooter>{footer}</StyledFooter> : <StyledFooter />}
        </Table>
      </Paper>
    </Wrapper>
  );
};

MuiTable.propTypes = {
  headers: PropTypes.arrayOf(PropTypes.string).isRequired,
  children: PropTypes.node.isRequired,
  title: PropTypes.string,
  isLoading: PropTypes.bool,
  footer: PropTypes.any,
};

export default MuiTable;
