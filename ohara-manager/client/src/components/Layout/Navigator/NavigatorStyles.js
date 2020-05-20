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
import ExpansionPanel from '@material-ui/core/ExpansionPanel';

export const StyledNavigator = styled.nav(
  ({ theme }) => css`
    width: 100%;
    height: 100%;
    background-color: ${theme.palette.primary.main};

    .button-wrapper {
      background-color: ${theme.palette.primary[500]};
    }

    .workspace-settings-menu {
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
    }
  `,
);

export const StyledExpansionPanel = styled(ExpansionPanel)(
  ({ theme }) => css`
    box-shadow: none;
    color: ${theme.palette.common.white};
    background-color: ${theme.palette.primary[500]};

    .MuiCollapse-container {
      /* 64px -> Mui's summary title height */
      height: calc(100% - 64px) !important;
    }

    /* 
      Ensure these elements are taking up full with and height and thus the 
      child element can have the full width/height
    */
    .MuiCollapse-wrapper,
    .MuiCollapse-wrapperInner,
    .MuiExpansionPanelDetails-root,
    .scrollbar-wrapper,
    div[role='region'] {
      height: 100%;
      width: 100%;
    }

    /* 
      TODO: Override react-scrollbars-custom's default styles with props or 
      custom styles
    */

    .scrollbar-wrapper {
      .ScrollbarsCustom-TrackY {
        top: 0 !important;
        height: 100% !important;
        width: 5px !important;
      }
    }

    .Mui-expanded {
      .new-pipeline-button {
        transition: transform 150ms cubic-bezier(0.4, 0, 0.2, 1) 0ms;
        top: 16px;
        transform: rotate(180deg);
      }
    }

    &.MuiExpansionPanel-root.Mui-expanded {
      margin: 0;
      height: calc(50% - 60px) !important;
    }

    /* Prevent an extra line when the expansion panel is closed...  */
    &.MuiExpansionPanel-root:before {
      height: 0;
    }

    .MuiExpansionPanelSummary-content {
      margin: 0;
    }

    .MuiExpansionPanelSummary-root {
      padding: 0 ${theme.spacing(2)}px;
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
      cursor: pointer;
    }

    .MuiExpansionPanelSummary-root.Mui-focused {
      /* Remove default focused color which looks a lot like a bug... */
      background-color: transparent;
    }

    .MuiExpansionPanelSummary-root:hover:not(.Mui-disabled) {
      /* Disable the title from having pointer cursor */
      cursor: auto;
    }
  `,
);
