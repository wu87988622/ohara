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
import Typography from '@material-ui/core/Typography';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';

import { Button } from 'components/common/Form';

export const StyledNavigator = styled.nav(
  ({ theme }) => css`
    min-width: 220px;
    width: 220px;
    background-color: ${theme.palette.primary[500]};
  `,
);

export const StyledButton = styled(Button)(
  ({ theme }) => css`
    font-size: 20px;
    margin: ${theme.spacing(2)}px;

    /* Reset button styles */
    background-color: transparent !important;
    box-shadow: none !important;
    font-weight: normal;
    text-transform: capitalize;
    padding: 0;

    .menu-name {
      height: 28px;
      overflow: hidden;
      letter-spacing: 1px;
      text-align: left;
      display: inline-block;
      /* Prevent a "half cut" text */
      word-break: break-all;
    }

    i {
      color: white;
      font-size: 18px;
      margin: 4px 0 0 ${theme.spacing(1)}px;
    }
  `,
);

export const StyledSubtitle1 = styled(Typography).attrs({
  variant: 'subtitle1',
})(
  ({ theme }) => css`
    color: ${theme.palette.common.white};
  `,
);

export const StyledExpansionPanel = styled(ExpansionPanel)(
  ({ theme }) => css`
    box-shadow: none;
    background-color: transparent;
    color: ${theme.palette.common.white};

    .Mui-expanded {
      .new-pipeline-button {
        transition: transform 150ms cubic-bezier(0.4, 0, 0.2, 1) 0ms;
        top: 16px;
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
      padding: 5px;
      position: absolute;
      right: 50px;
      top: 8px;
      width: 30px;
      height: 30px;
    }
  `,
);

export const PipelineList = styled.ul(
  ({ theme }) => css`
    width: 100%;

    li {
      display: flex;
      justify-content: center;
      align-items: center;
      color: ${theme.palette.common.white};
      line-height: 36px;
      font-size: 14px;

      a {
        color: ${theme.palette.common.white};
        text-decoration: none;
        width: 100%;
        padding: 0 26px;
        display: flex;
        align-items: center;

        &:hover {
          background-color: rgba(255, 255, 255, 0.3);
        }

        &.active-link {
          background-color: rgba(255, 255, 255, 0.3);
        }

        .link-icon {
          margin-right: ${theme.spacing(1)}px;
          width: 16px;
        }
      }
    }
  `,
);
