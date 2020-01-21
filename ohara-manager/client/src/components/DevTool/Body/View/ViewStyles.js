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

import styled, { css } from 'styled-components';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';

export const StyledTableRow = styled(TableRow)`
  height: 50px;
`;

export const StyledTableCell = styled(TableCell)`
  max-width: 50px;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
`;

export const StyledTableErrorCell = styled(TableCell)(
  ({ theme }) => css`
    background-color: ${theme.palette.common.white};

    /* we only change the text color of "the first cell" right after the "view icon" element */
    + .error-message {
      color: ${theme.palette.text.disabled};
    }
  `,
);

export const StyledTopicView = styled.div`
  textarea {
    width: 100%;
    height: 200px;
    resize: none;
  }
`;

export const StyledContent = styled.div`
  padding-bottom: ${props => props.theme.spacing(1)}px;
`;
