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

export const PaperWrapper = styled.div`
  position: relative;

  /* Subtract the height of Toolbar  */
  height: calc(100vh - 72px);
`;

export const Paper = styled.div(
  ({ theme }) => css`
    border: ${theme.spacing(1)}px solid ${theme.palette.common.white};
    overflow: hidden;
    cursor: grab;

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

      .connectorMenu {
        position: absolute;
        top: ${theme.spacing(0.5)}px;
        left: calc(100% + 8px);

        svg {
          color: ${theme.palette.grey[600]};
        }

        button {
          pointer-events: auto;
          background-color: transparent;
          border: 0;
          padding: 0;

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
      }

      .circle {
        width: 40px;
        height: 40px;
        border-radius: 100%;
        background-color: ${theme.palette.grey[600]};
        display: flex;
        align-items: center;
        justify-content: center;
        margin: ${theme.spacing(2)}px;

        svg {
          color: white;
        }
      }
      .title {
        ${theme.typography.h5}
        color:${theme.palette.text.primary};
        margin-top: ${theme.spacing(2)}px;
      }

      .type {
        ${theme.typography.body2}
        color:${theme.palette.text.secondary};
      }
      
      .status {
        display: flex;
        justify-content: space-between;
        border-top: 1px solid ${theme.palette.divider};
        width: 100%;
        padding: ${theme.spacing(0.5, 2, 0, 2)};
      }
    }

    .topic {
      position: absolute;
      pointer-events: none;
      background-color: white;

      .title {
        ${theme.typography.h5}
        color:${theme.palette.text.primary};
        text-align:center;
        
}
      .topicMenu {
        width: 24px;
        position: absolute;
        top: ${theme.spacing(0.5)}px;
        left: calc(100% + 8px);

        svg {
          color: ${theme.palette.grey[600]};
        }

        button {
          pointer-events: auto;
          background-color: transparent;
          border: 0;
          padding: 0;

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
    }
  `,
);
