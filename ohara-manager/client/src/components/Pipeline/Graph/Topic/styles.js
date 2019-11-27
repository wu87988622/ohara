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

import { css } from 'styled-components';

export const topic = () => css`
  position: absolute;
  pointer-events: none;
  background-color: white;
`;

export const title = ({ theme }) => css`
  ${theme.typography.h5}
  color:${theme.palette.text.primary};
  text-align:center;
`;

export const topicMenu = ({ theme }) => css`
  width: 24px;
  position: absolute;
  top: ${theme.spacing(1)}px;
  left: 61px;

  svg {
    color: ${theme.palette.grey[500]};
  }
`;
