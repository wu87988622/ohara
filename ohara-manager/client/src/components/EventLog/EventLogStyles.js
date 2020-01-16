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
    position: relative;
    width: 100%;
    height: 100%;

    .logs {
      position: absolute;
      height: calc(100% - ${theme.spacing(6)}px - ${theme.spacing(3.25)}px);
      width: 100%;
      top: ${theme.spacing(6)}px;
      bottom: ${theme.spacing(3.25)}px;
    }

    .status-bar {
      position: absolute;
      bottom: 0;
      width: 100%;
    }
  `,
);
