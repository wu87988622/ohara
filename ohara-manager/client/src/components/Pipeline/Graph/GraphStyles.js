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
import {
  connector,
  circle,
  title,
  type,
  status,
  left,
  right,
  menu,
} from './Connector/styles';

export const Paper = styled.div(
  ({ theme }) => css`
    border: ${theme.spacing(1)}px solid #fff;
    overflow: hidden;

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
      ${connector}
    }
    .circle {
      ${circle}

      svg {
        color: white;
      }
    }
    .title {
      ${title}
    }
    .type {
      ${type}
    }
    .status {
      ${status}
      .left {
        ${left}
      }
      .right {
        ${right}
      }
    }
    .menu {
      ${menu}

      button {
        pointer-events: auto;
        background-color: transparent;
        border: 0;
        padding: 0;

        &:hover {
          background-color: rgba(0, 0, 0, 0.08);
        }

        &:focus {
          outline: 0;
        }
      }
    }
  `,
);
