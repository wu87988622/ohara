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

    &.is-open {
      display: block;
    }

    .title {
      padding: ${theme.spacing(1)}px ${theme.spacing(2)}px;
      background-color: #f5f6fa;

      &.box-title {
        cursor: move;
        display: flex;
        align-items: center;

        button {
          margin-left: auto;
        }
      }
    }

    .add-button {
      display: flex;
      align-items: center;
      button {
        margin-right: ${theme.spacing(1)}px;
      }
    }

    .detail {
      padding: ${theme.spacing(1)}px ${theme.spacing(2)}px;
    }

    .panel-title {
      background-color: #f5f6fa;
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
  `,
);
