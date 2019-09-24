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
import Paper from '@material-ui/core/Paper';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';

import { H4 } from 'components/common/Mui/Typography';

export const Box = styled(Paper)`
  margin-bottom: 20px;
  padding: 25px;
`;

export const StyledH4 = styled(H4)`
  margin-top: 0;

  i {
    margin-right: ${props => props.theme.spacing(1)}px;
  }
`;

export const ExpandIcon = styled.i`
  font-size: 16px;
`;

export const StyledExpansionPanel = styled(ExpansionPanel)`
  position: relative;
  box-shadow: none;

  .MuiExpansionPanelDetails-root {
    display: block;
    padding: 0;
  }

  .MuiExpansionPanelSummary-root {
    padding: 0;
  }

  .MuiExpansionPanelSummary-content {
    display: block;
    margin: 0;
    cursor: auto;
  }

  .MuiExpansionPanelSummary-expandIcon {
    position: absolute;
    right: 0;
    top: 0;
  }
`;

export const NodeNamesList = styled.ul`
  .item {
    color: ${props => props.theme.palette.text.secondary};
  }

  .item-header {
    font-size: 11px;
    margin-bottom: ${props => props.theme.spacing(1)}px;
  }

  .item-body {
    font-size: 14px;
    display: flex;
    margin-bottom: ${props => props.theme.spacing(2)}px;
    border-bottom: 1px solid ${props => props.theme.palette.text.secondary};
  }

  .item-value {
    margin-bottom: ${props => props.theme.spacing(1)}px;
  }

  .item-unit {
    margin-left: auto;
  }
`;
