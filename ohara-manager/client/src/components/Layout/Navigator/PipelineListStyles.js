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

export const Wrapper = styled.ul(
  ({ theme }) => css`
    width: 100%;

    li {
      display: flex;
      justify-content: center;
      align-items: center;
      color: ${theme.palette.common.white};
      line-height: 36px;
      font-size: 14px;

      a {
        color: ${theme.palette.common.white};
        text-decoration: none;
        width: 100%;
        padding: 0 ${theme.spacing(3)}px;
        display: flex;
        align-items: center;

        &:hover {
          background-color: rgba(255, 255, 255, 0.3);
        }

        &.active-link {
          background-color: rgba(255, 255, 255, 0.3);
        }

        .link-icon {
          margin-right: ${theme.spacing(1)}px;
          width: 20px;
        }
      }
    }
  `,
);
