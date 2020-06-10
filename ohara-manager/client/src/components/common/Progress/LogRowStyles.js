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

    .log-icon {
      position: absolute;
      left: 5px;
      top: 5px;
      font-size: 15px;
    }

    &.warning {
      color: ${theme.palette.warning.dark};
    }

    &.error {
      color: ${theme.palette.error.main};
      a {
        color: inherit;
        cursor: pointer;
      }
    }

    .date {
      min-width: ${theme.spacing(15)}px;
      text-align: right;
    }
  `,
);
