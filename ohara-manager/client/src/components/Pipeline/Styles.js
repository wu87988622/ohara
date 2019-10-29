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
import Typography from '@material-ui/core/Typography';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';

import { Button } from 'components/common/Form';

export const Navigator = styled.nav`
  min-width: 220px;
  width: 220px;
  background-color: ${props => props.theme.palette.primary[500]};
`;

export const StyledButton = styled(Button)`
  font-size: 20px;
  margin: ${props => props.theme.spacing(2)}px;

  /* Reset button styles */
  background-color: transparent !important;
  box-shadow: none !important;
  font-weight: normal;
  text-transform: capitalize;
  padding: 0;

  .menu-name {
    width: 160px;
    overflow: hidden;
    letter-spacing: 1px;
    text-align: left;
  }

  i {
    color: white;
    font-size: 18px;
    margin: 4px 0 0 ${props => props.theme.spacing(1)}px;
  }
`;

export const StyledSubtitle1 = styled(Typography).attrs({
  variant: 'subtitle1',
})`
  color: ${props => props.theme.palette.common.white};
`;

export const StyledExpansionPanel = styled(ExpansionPanel)`
  box-shadow: none;
  background-color: transparent;
  color: ${props => props.theme.palette.common.white};

  .Mui-expanded {
    .new-pipeline-button {
      transition: transform 150ms cubic-bezier(0.4, 0, 0.2, 1) 0ms;
      top: 26px;
      transform: rotate(180deg);
    }
  }

  &.MuiExpansionPanel-root.Mui-expanded {
    margin: 0;
  }

  /* Prevent an extra line when the expansion panel is closed...  */
  &.MuiExpansionPanel-root:before {
    height: 0;
  }

  .MuiExpansionPanelSummary-content {
    margin: 0;
  }

  .MuiExpansionPanelSummary-root {
    padding: 0 20px;
    position: relative;
  }

  .MuiExpansionPanelSummary-expandIcon {
    background-color: transparent !important;
    box-shadow: none !important;
    color: white;
  }

  .MuiExpansionPanelDetails-root {
    padding: 0;
  }

  &:first-child,
  &:last-child {
    border-radius: 0;
  }

  .new-pipeline-button {
    transform: rotate(0deg);
    transition: transform 150ms cubic-bezier(0.4, 0, 0.2, 1) 0ms;
    margin-left: 20px;
    position: absolute;
    right: 50px;
    top: 16px;
    font-size: 15px;
  }
`;

export const PipelineList = styled.ul`
  width: 100%;

  li {
    display: flex;
    justify-content: center;
    align-items: center;
    color: ${props => props.theme.palette.common.white};
    line-height: 36px;
    font-size: 14px;

    a {
      color: ${props => props.theme.palette.common.white};
      text-decoration: none;
      width: 100%;
      padding: 0 26px;

      &:hover {
        background-color: rgba(255, 255, 255, 0.3);
      }

      &.active-link {
        background-color: rgba(255, 255, 255, 0.3);
      }

      i {
        margin-right: ${props => props.theme.spacing(1)}px;
      }
    }
  }
`;
