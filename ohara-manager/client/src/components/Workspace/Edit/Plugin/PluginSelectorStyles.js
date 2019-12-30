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
import DialogContent from '@material-ui/core/DialogContent';
import DialogTitle from '@material-ui/core/DialogTitle';
import DialogActions from '@material-ui/core/DialogActions';

export const StyledDialogTitle = styled(DialogTitle)(
  ({ theme }) => css`
    cursor: move;
    .close-button {
      position: absolute;
      right: ${theme.spacing(1)}px;
      top: ${theme.spacing(1)}px;
      color: ${theme.palette.grey[500]};
    }
  `,
);

export const StyledDialogContent = styled(DialogContent)(
  ({ theme }) => css`
    width: ${theme.spacing(54)}px;
  `,
);

export const StyledDialogActions = styled(DialogActions)(
  ({ theme }) => css`
    padding: ${theme.spacing(2)}px ${theme.spacing(3)}px;
  `,
);

export const FileFilter = styled.div(
  ({ theme }) => css`
    width: ${theme.spacing(30)}px;
  `,
);

export const FileList = styled.div(
  ({ theme }) => css`
    min-height: calc(100vh - ${theme.spacing(40)}px);
    .checkboxes {
      margin-left: ${theme.spacing(3)}px;
    }
  `,
);
