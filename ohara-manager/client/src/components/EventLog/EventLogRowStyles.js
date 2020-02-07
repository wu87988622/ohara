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

export default styled.div(
  ({ theme }) => css`
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding: ${theme.spacing(0.5, 1.5, 0.25, 3)};
    border-bottom: 1px solid ${theme.palette.divider};

    &.error {
      color: ${theme.palette.error.main};
      a {
        cursor: pointer;
      }

      :before {
        position: absolute;
        margin-left: -${theme.spacing(2)}px;
        margin-top: ${theme.spacing(0.5)}px;
        content: '+';
        transform: rotate(45deg);
        height: 10px;
        width: 10px;
        color: white;
        background: ${theme.palette.error.main};
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 100%;
      }
    }

    .date {
      min-width: ${theme.spacing(15)}px;
      text-align: right;
    }
  `,
);
