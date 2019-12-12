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
    .services {
      .MuiButton-root {
        min-width: 0;
      }
      .MuiIconButton-root {
        padding: 0;
      }
      .MuiButton-root {
        text-transform: none;
      }

      .expand {
        transform: rotate(0deg);
        margin-left: auto;
        transition: ${theme.transitions.create('transform', {
          duration: theme.transitions.duration.shortest,
        })};
      }
      .expand-open {
        transform: rotate(180deg);
      }
    }
    .services-detail {
      .MuiChip-root {
        height: 20px;
        border-radius: 4px;
      }
      .service-name:hover {
        background-color: rgba(0, 0, 0, 0.08);
      }
    }
  `,
);
