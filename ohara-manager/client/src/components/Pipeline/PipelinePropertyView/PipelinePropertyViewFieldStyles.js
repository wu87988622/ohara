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

export const FieldWrapper = styled.div(
  ({ theme }) => css`
    position: relative;
    margin-bottom: ${theme.spacing(2)}px;
    display: flex;

    .field {
      display: flex;
      flex-direction: column;
      width: 100%;

      .field-label {
        &:first-letter {
          text-transform: uppercase;
        }
      }

      .field-value {
        width: calc(100% - 20%);
        white-space: nowrap;
        text-overflow: ellipsis;
        overflow: hidden;
      }
    }

    .docs-tooltip {
      position: absolute;
      right: 4px;
      top: 0;

      svg {
        font-size: ${theme.typography.body1.fontSize};
      }
    }

    .node-status,
    .metrics-unit {
      position: absolute;
      right: 0;
      bottom: 0;
      text-transform: lowercase;
    }
  `,
);
