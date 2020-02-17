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

export const StyledPaper = styled.div(
  ({ theme }) => css`
    border: ${theme.spacing(1)}px solid ${theme.palette.common.white};
    overflow: hidden;
    cursor: grab;
    width: 100%;
    height: 100%;

    &.is-being-grabbed {
      cursor: grabbing;
    }

    .flying-paper {
      border: 1px dashed ${theme.palette.grey[400]};
      box-shadow: ${theme.shadows[8]};
      display: flex;
      align-items: center;
      opacity: 0.85;
      z-index: ${theme.zIndex.flyingPaper};
      padding: ${theme.spacing(0, 2)};
      font-size: ${theme.typography.body1};

      &.flying-topic {
        border-radius: 100%;
        color: ${theme.palette.grey[600]};

        .item {
          .icon {
            margin-right: 0;
            line-height: 0;
          }
        }

        .display-name {
          display: none;
        }
      }

      .item {
        height: auto !important;
        display: flex;
        align-items: center;
        width: 100%;

        .icon {
          margin-right: ${theme.spacing(1)}px;
        }

        .display-name {
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }
    }

    svg .link {
      z-index: 2;
    }

    .connector {
      background-color: white;
      position: absolute;
      border: 1px solid ${theme.palette.divider};
      border-radius: ${theme.shape.borderRadius}px;
      pointer-events: none;

      &:hover {
        box-shadow: 0 0 0 2px ${theme.palette.primary};
      }

      .menu {
        margin-top: 5px;
        display: none;
        height: 100%;

        svg {
          color: ${theme.palette.grey[600]};
        }

        button {
          pointer-events: auto;
          background-color: transparent;
          border: 0;
          padding: 0;
          margin-right: ${theme.spacing(1)}px;

          &.is-disabled {
            pointer-events: none;
            opacity: 0.3;
          }

          &:hover {
            cursor: pointer;

            svg {
              background-color: ${theme.palette.action.hover};
              border-radius: ${theme.shape.borderRadius}px;
            }
          }

          &:focus {
            outline: 0;
          }
        }
      }

      .header {
        display: flex;
        align-items: center;
        height: 65px;
        border-bottom: 1px solid ${theme.palette.divider};
      }

      .body {
        line-height: 35px;
        height: 35px;
        margin: 0 ${theme.spacing(2)}px;
      }

      .icon {
        width: 40px;
        height: 40px;
        border-radius: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0 ${theme.spacing(2)}px;

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
          color: white;
        }
      }

      .display-name {
        font-size: ${theme.typography.h5};
        color: ${theme.palette.text.primary};
        width: 150px;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .type {
        font-size: ${theme.typography.body2};
        color: ${theme.palette.text.secondary};
      }

      .metrics {
        .field {
          display: flex;
          border-bottom: 1px solid ${theme.palette.divider};
        }

        .field-name {
          &:first-letter {
            text-transform: capitalize;
          }
        }

        .field:last-child {
          border-bottom: none;
        }

        .field-value {
          margin-left: auto;
        }
      }

      .status {
        display: flex;
        justify-content: space-between;
        width: 100%;
      }

      .status-value {
        text-transform: lowercase;

        &:first-letter {
          text-transform: capitalize;
        }
      }
    }

    .topic {
      position: absolute;
      pointer-events: none;
      background-color: white;
      display: flex;
      flex-direction: column;
      align-items: center;
      padding-top: ${theme.spacing(1)}px;

      .display-name {
        font-size: ${theme.typography.h5};
        color: ${theme.palette.text.primary};
        text-align: center;
        margin-top: 2px;
      }

      .menu {
        position: absolute;
        bottom: 0;
        left: 0;
        width: 100%;
        display: none;
        padding: 5px 0;

        svg {
          color: ${theme.palette.grey[600]};
        }

        button {
          pointer-events: auto;
          background-color: transparent;
          border: 0;
          padding: 0;

          &.is-disabled {
            pointer-events: none;
            opacity: 0.3;
          }

          &:hover {
            cursor: pointer;

            svg {
              background-color: ${theme.palette.action.hover};
              border-radius: ${theme.shape.borderRadius}px;
            }
          }

          &:focus {
            outline: 0;
          }
        }
      }

      .menu-inner {
        background-color: ${theme.palette.common.white};
        display: flex;
        align-items: center;
        justify-content: space-evenly;
      }
    }
  `,
);
