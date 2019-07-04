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
import { FormGroup, Label } from 'components/common/Form';
import { primaryBtn } from 'theme/btnTheme';
import { Button } from 'components/common/Mui/Form';
import Fab from '@material-ui/core/Fab';
import Icon from '@material-ui/core/Icon';
import DialogContent from '@material-ui/core/DialogContent';
import Paper from '@material-ui/core/Paper';

const List = styled.div`
  position: relative;
  width: ${props => props.width || '100%'};
  border: 1px solid ${props => props.theme.lighterGray};
  padding: 0.5rem 1rem;
  border-radius: 0.25rem;
  min-height: 8rem;
`;

const ListItem = styled.div`
  margin: 0.25rem 0;
  font-size: 13px;
  color: ${props => props.theme.lightBlue};
`;

const FormRow = styled(FormGroup).attrs({
  isInline: true,
})`
  margin: ${props => props.margin || 0};
`;

const FormCol = styled(FormGroup)`
  width: ${props => props.width || '100%'};
`;

const AppendButton = styled(Button).attrs({
  theme: primaryBtn,
})`
  position: absolute;
  right: 0;
  top: -3rem;
`;

const StyledButton = styled(Button)`
  position: absolute;
  right: 0;
  top: -3rem;
`;

const StyledIconButton = styled(Fab)`
  position: absolute;
  right: 0;
  top: -3rem;
`;

const ActionIcon = styled(Icon)`
  font-size: 20px;
`;

const StyledDialogContent = styled(DialogContent)`
  padding-top: 30px;
`;

const StyledInputFile = styled.input`
  display: none;
`;

const StyledLabel = styled(Label)`
  position: absolute;
  right: 0;
  top: -3rem;
`;

const StyledPaper = styled(Paper)`
  min-height: 100px;
`;

export {
  List,
  ListItem,
  FormRow,
  FormCol,
  AppendButton,
  StyledButton,
  StyledIconButton,
  ActionIcon,
  StyledDialogContent,
  StyledInputFile,
  StyledLabel,
  StyledPaper,
};
