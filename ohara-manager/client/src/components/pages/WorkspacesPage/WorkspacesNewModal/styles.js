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

import styled from 'styled-components';
import DialogContent from '@material-ui/core/DialogContent';
import Paper from '@material-ui/core/Paper';

import { Label } from 'components/common/Form';
import { Warning } from 'components/common/Messages';

export const StyledDialogContent = styled(DialogContent)`
  padding-top: 30px;
`;

export const StyledDialogDividers = styled(DialogContent)`
  padding-left: 5px;
  padding-right: 5px;
`;

export const StyledInputFile = styled.input`
  display: none;
`;

export const StyledLabel = styled(Label)`
  position: absolute;
  right: 0;
  top: -3rem;
`;

export const StyledPaper = styled(Paper)`
  min-height: 100px;
`;

export const StyledWarning = styled(Warning)`
  padding: 30px 20px;
`;
