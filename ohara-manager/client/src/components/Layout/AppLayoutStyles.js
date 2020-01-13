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
    .Resizer {
      background: ${theme.palette.common.black};
      opacity: 0.2;
      z-index: 1;
      background-clip: padding-box;
    }

    .Resizer:hover {
      transition: all 2s ease;
    }

    .Resizer.horizontal {
      height: ${theme.spacing(1)}px;
      margin: -${theme.spacing(0.5)}px 0;
      border-top: ${theme.spacing(0.5)}px solid ${theme.palette.transparent};
      border-bottom: ${theme.spacing(0.5)}px solid ${theme.palette.transparent};
      cursor: row-resize;
      width: 100%;
    }

    .Resizer.horizontal:hover {
      border-top: ${theme.spacing(0.5)}px solid ${theme.palette.action.hover};
      border-bottom: ${theme.spacing(0.5)}px solid ${theme.palette.action.hover};
    }

    .Resizer.vertical {
      width: ${theme.spacing(1)}px;
      margin: 0 -${theme.spacing(0.5)}px;
      border-left: ${theme.spacing(0.5)}px solid ${theme.palette.transparent};
      border-right: ${theme.spacing(0.5)}px solid ${theme.palette.transparent};
      cursor: col-resize;
    }

    .Resizer.vertical:hover {
      border-left: ${theme.spacing(0.5)}px solid ${theme.palette.action.hover};
      border-right: ${theme.spacing(0.5)}px solid ${theme.palette.action.hover};
    }

    .Resizer.disabled {
      cursor: default;
    }
  `,
);
