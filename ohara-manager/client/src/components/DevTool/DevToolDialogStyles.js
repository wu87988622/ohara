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

export const StyledDevTool = styled.div(
  ({ theme }) => css`
    position: absolute;
    /* AppBar width */
    left: 70px;
    width: calc(100% - 70px);
    min-width: 956px;
    height: 50%;

    /* We need to leave some space for StatusBar */
    bottom: 26px;
    z-index: ${theme.zIndex.modal};
    background-color: ${theme.palette.common.white};

    &.is-close {
      display: none;
    }

    .header {
      width: 100%;
      height: 48px;
      background-color: ${theme.palette.grey[50]};
    }
  `,
);
