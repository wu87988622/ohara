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
    .MuiListItem-root {
      &.Mui-selected {
        background-color: ${theme.palette.primary[50]};

        .MuiListItemText-root {
          color: ${theme.palette.primary[600]};
        }
      }
    }

    .nested {
      padding-left: ${theme.spacing(3)}px;

      .MuiListItemText-root {
        padding-left: ${theme.spacing(3)}px;
      }

      ::before {
        content: '';
        left: ${theme.spacing(3)}px;
        top: 0;
        bottom: 0;
        position: absolute;
        width: ${theme.spacing(0.25)}px;
        background-color: ${theme.palette.grey[300]};
      }

      :first-child::before {
        margin-top: ${theme.spacing(1.5)}px;
      }

      :last-child::before {
        margin-bottom: ${theme.spacing(1.5)}px;
      }

      &.Mui-selected {
        background-color: white;

        .MuiListItemText-root {
          border-left: ${theme.palette.primary[600]} ${theme.spacing(0.25)}px
            solid;
          z-index: 0;
        }
      }
    }
  `,
);
