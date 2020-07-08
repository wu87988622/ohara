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
    height: ${theme.spacing(56)}px;

    .workspace-icon {
      border-radius: 10%;
    }

    .call-to-action {
      margin: 0 auto;
    }

    .active {
      border: 2px solid ${theme.palette.primary[400]};
    }

    .inactive:hover {
      border: 2px solid ${theme.palette.grey[400]};
    }

    .unstable:hover {
      border: none;
    }
  `,
);
