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

export const StyledToolbar = styled.div(
  ({ theme }) => css`
    height: 72px;
    padding: ${theme.spacing(1, 3, 0)};
    background-color: ${theme.palette.grey[100]};
    border-bottom: 1px solid ${theme.palette.divider};
    display: flex;
    align-items: center;

    .toolbox-controls,
    .paper-controls,
    .pipeline-controls {
      /* Customize button pseudo states  */
      button {
        background-color: ${theme.palette.common.white};

        &:hover {
          background-color: ${theme.palette.action.hover};
        }

        &.Mui-disabled {
          .MuiSvgIcon-colorAction {
            color: ${theme.palette.action.disabledBackground};
          }
        }
      }
    }

    .toolbox-controls {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-right: ${theme.spacing(10)}px;
    }

    .paper-controls {
      display: flex;
      margin-right: ${theme.spacing(10)}px;

      /* Remove border between buttons */
      button:not(:first-child) {
        border-left: none;
      }

      .zoom,
      .fit,
      .center {
        display: flex;
        flex-direction: column;
        align-items: center;
        margin-right: ${theme.spacing(1)}px;
      }

      .center {
        margin-right: 0;
      }
    }

    .pipeline-controls {
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .metrics-controls {
      /* Move this to the very right */
      margin-left: auto;
    }
  `,
);
