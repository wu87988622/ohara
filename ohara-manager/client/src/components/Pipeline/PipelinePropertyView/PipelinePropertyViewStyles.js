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
import Paper from '@material-ui/core/Paper';

export const Wrapper = styled(Paper)(
  ({ theme }) => css`
    width: 100%;
    height: 100%;
    position: absolute;
    overflow-y: auto;
    z-index: ${theme.zIndex.propertyView};

    .title-wrapper {
      display: flex;
      align-items: center;
      border-bottom: 1px solid ${theme.palette.divider};
      padding: ${theme.spacing(2)}px;
    }

    .title-info {
      display: flex;
    }

    .icon-wrapper {
      width: 16px;
      height: 16px;
      border-radius: 100%;
      background-color: #757575;
      margin: ${theme.spacing(0.25, 1, 0, 0)};
      display: flex;
      align-items: center;
      justify-content: center;

      &.stopped {
        background-color: ${theme.palette.grey[600]};
      }

      &.pending {
        background-color: ${theme.palette.warning.main};
      }

      &.running {
        background-color: ${theme.palette.success.main};
      }

      &.failed {
        background-color: ${theme.palette.error.main};
      }

      svg {
        color: ${theme.palette.common.white};
        width: 12px;
      }
    }

    .status-key {
      margin-right: ${theme.spacing(0.5)}px;
    }

    .status-value {
      text-transform: lowercase;
    }

    .close-button {
      margin-left: auto;
      margin-top: -${theme.spacing(1)}px;
    }

    .section-title {
      margin-left: ${theme.spacing(1)}px;
    }

    /* Settings */
    .settings {
      height: 100%;
      overflow-y: auto;
    }

    .MuiExpansionPanel-root {
      /* We don't want the default shadow */
      box-shadow: none;
      border-bottom: 1px solid ${theme.palette.divider};
    }

    .MuiExpansionPanelSummary-content {
      align-items: center;
    }

    .MuiExpansionPanelDetails-root {
      /* Mui's default "display: flex" breaks our layout */
      display: block;
    }

    .field-wrapper {
      margin-bottom: ${theme.spacing(2)}px;
    }

    .MuiTreeView-root {
      /* Need some negative margin to ensure the component is aligned properly */
      margin-left: -${theme.spacing(1)}px;

      &:focus > .MuiTreeItem-content,
      .MuiTreeItem-content {
        background-color: transparent;
      }

      .MuiTreeItem-label {
        font-size: ${theme.typography.body2.fontSize};
      }
    }
  `,
);
