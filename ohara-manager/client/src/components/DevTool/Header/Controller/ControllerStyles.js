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
import TextField from '@material-ui/core/TextField';

import { TAB } from 'context/devTool/const';

export const StyledSearchBody = styled.div`
  width: 280px;
  height: ${props => (props.tab === TAB.topic ? '210px' : '440px')};
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: stretch;
  background-color: ${props => props.theme.palette.common.white};
  padding: ${props => props.theme.spacing(3)}px;

  label {
    font-weight: bold;
  }

  button {
    width: 88px;
    height: 36px;
    background-color: ${props => props.theme.palette.primary[600]};
    align-self: flex-end;
  }
`;

export const StyledTextField = styled(TextField)`
  width: 230px;
`;
