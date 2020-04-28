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

export const Wrapper = styled.div(
  ({ theme }) => css`
    width: 680px;
    margin: auto;

    section > h2 {
      margin: ${theme.spacing(3, 0)};

      &:first-child {
        /* Ensure the section aligns with the left-hand side navigation */
        margin-top: ${theme.spacing(1, 0)}px;
      }
    }

    .section-wrapper {
      display: none;

      &.should-display {
        display: block;
      }
    }

    .MuiList-padding {
      padding-top: 0;
      padding-bottom: 0;

      .MuiListItem-root {
        padding: 0;
        cursor: pointer;
        max-width: none;
        min-width: auto;
        min-height: auto;
        border-bottom: 1px solid ${theme.palette.divider};

        &:hover {
          background-color: ${theme.palette.action.hover};
        }

        &:last-child {
          border-bottom: none;
        }
      }
      .MuiListItemIcon-root {
        min-width: ${theme.spacing(5)}px;
      }
      .MuiListItemText-root {
        margin: 0;
      }
    }

    .MuiBadge-root {
      display: flex;
      width: 100%;
      padding: ${theme.spacing(1, 2)};

      .MuiBadge-badge {
        color: ${theme.palette.common.white};
        background-color: ${theme.palette.warning.main};
      }
    }

    .list-wrapper {
      border: 1px solid ${theme.palette.divider};
      border-radius: ${theme.shape.borderRadius}px;
      background-color: ${theme.palette.background.paper};

      &.is-danger-zone {
        border-color: ${theme.palette.error.main};
      }
    }

    .section-page-header {
      display: flex;
      align-items: center;
      margin: ${theme.spacing(2, 0)};

      .section-page-content {
        padding: ${theme.spacing(0, 2)};
      }

      .MuiTypography-root {
        margin-left: ${theme.spacing(2)}px;
      }
    }
  `,
);
