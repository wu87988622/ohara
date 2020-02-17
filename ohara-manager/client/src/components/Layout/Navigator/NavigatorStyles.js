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

import { Button } from 'components/common/Form';

export const StyledNavigator = styled.nav(
  ({ theme }) => css`
    width: 100%;
    height: 100%;
    background-color: ${theme.palette.primary.main};

    .button-wrapper {
      background-color: ${theme.palette.primary[500]};
    }
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
        padding: 0 ${theme.spacing(3)}px;
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
          width: 20px;
        }
      }
    }
  `,
);

export const StyledOutlineList = styled.div(
  ({ theme }) => css`
    .scrollbar-wrapper {
      /* 58px -> outline heading height */
      height: calc(100% - 58px);

      /* 
        TODO: Override react-scrollbars-custom's default styles with props or 
        custom styles
       */

      .ScrollbarsCustom-TrackY {
        top: 0 !important;
        height: 100% !important;
        width: 5px !important;
      }
    }

    h5 {
      padding: 0 ${theme.spacing(2)}px;
      color: ${theme.palette.common.white};
      padding: ${theme.spacing(2)}px;
      border-top: 1px solid ${theme.palette.primary[400]};
      border-bottom: 1px solid ${theme.palette.primary[400]};
      background-color: ${theme.palette.primary[500]};

      display: flex;
      align-items: center;

      .MuiSvgIcon-root {
        margin-right: ${theme.spacing(1)}px;
      }
    }

    li {
      color: ${theme.palette.common.white};
      display: flex;
      align-items: center;
      color: ${theme.palette.common.white};
      line-height: 36px;
      font-size: 14px;
      padding: ${theme.spacing(0, 3)};
      cursor: pointer;

      &:hover,
      &.is-selected {
        background-color: rgba(255, 255, 255, 0.3);
      }

      /* The custom icon needs some more padding in order to be aligned 
        with the rest of items
      */
      &.is-shared {
        padding-left: 26px;
      }

      > svg {
        margin-right: ${theme.spacing(1)}px;
        width: 20px;
      }
    }
  `,
);
