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

export const StyledToolbox = styled.div(
  ({ theme }) => css`
    position: absolute;
    top: ${theme.spacing(1)}px;
    left: ${theme.spacing(1)}px;
    width: 272px;
    background-color: ${theme.palette.common.white};
    box-shadow: ${theme.shadows[24]};
    display: none;
    z-index: ${theme.zIndex.toolbox};

    &.is-open {
      display: block;
    }

    .title {
      padding: ${theme.spacing(1)}px ${theme.spacing(2)}px;
      background-color: ${theme.palette.grey[100]};

      &.toolbox-title {
        cursor: move;
        display: flex;
        align-items: center;

        button {
          margin-left: auto;
        }
      }
    }

    .toolbox-body {
      overflow-y: auto;

      /* 
        Prevent the horizontal scrollbar from showing up during expanding/collapsing
        a tool section
      */
      overflow-x: hidden;
    }

    .add-button {
      display: flex;
      align-items: center;
      padding: ${theme.spacing(1)}px;

      button {
        margin-right: ${theme.spacing(1)}px;
      }

      input {
        display: none;
      }

      label {
        cursor: pointer;
        height: 24px;
      }
    }

    .detail {
      padding: 0;
      display: block;

      .MuiListItemText-primary {
        font-size: ${theme.typography.subtitle2.fontSize};
      }

      .MuiListItemIcon-root {
        min-width: 32px;
      }
    }

    .MuiExpansionPanelSummary-root {
      padding: ${theme.spacing(0, 2)};
      background-color: ${theme.palette.grey[100]};
    }

    .MuiExpansionPanel-root {
      box-shadow: none;
    }

    .MuiExpansionPanelSummary-root.Mui-expanded {
      min-height: 52px;
    }
    .MuiExpansionPanelSummary-content.Mui-expanded,
    .MuiExpansionPanel-root.Mui-expanded {
      margin: 0;
    }

    .MuiExpansionPanel-root:before {
      opacity: 1;
    }

    .MuiExpansionPanel-root.Mui-expanded:before {
      opacity: 1;
    }

    /* Toolbox list */
    .toolbox-list {
      /*
        Ensure these skeletons are centered in the list section, these only appear
        when the inspect worker API is still in the air
      */
      .MuiSkeleton-root {
        margin: 0 auto;
      }

      .item {
        display: flex;
        align-items: center;
        width: 100%;
        padding: ${theme.spacing(1, 2)};
        color: ${theme.palette.grey[600]};

        /* Shared topic uses a custom icon which is not aligned properly with
          Mui's, so we're using a different padding here
         */
        &.shared {
          padding-left: 18px;
        }

        &.is-disabled {
          opacity: 0.38;
        }

        .display-name {
          font-size: ${theme.typography.h6.fontSize};
          color: ${theme.palette.text.primary};
        }

        .icon {
          display: flex;
        }
      }

      svg {
        margin-right: ${theme.spacing(1)}px;
      }

      .shared-topic {
        /* Need to hard-code this since the margin is needed in order 
           to align this icon with the pipeline-only icon
        */
        margin-left: 2px;
      }
    }
  `,
);
