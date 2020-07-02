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
    .stepper {
      padding: ${theme.spacing(3, 0)};
    }

    .logViewer {
      height: 320px;
      padding: ${theme.spacing(1, 2)};
      overflow-y: auto;
    }

    .divider {
      margin: ${theme.spacing(1, 0)};
      height: 1px;
      border-top: 1px solid ${theme.palette.grey[200]};
      text-align: center;

      span {
        position: relative;
        bottom: ${theme.spacing(1)}px;
        padding: ${theme.spacing(0, 2.5)};
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.8px;
        color: ${theme.palette.text.primary};
        background: ${theme.palette.common.white};
      }
    }

    .log-step-stage {
      cursor: pointer;
    }
  `,
);
