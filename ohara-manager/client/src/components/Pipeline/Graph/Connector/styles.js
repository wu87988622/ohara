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

export const connector = ({ theme }) => css`
  background-color: white;
  position: absolute;
  border-color: ${theme.palette.divider};
  border: 1px solid ${theme.palette.divider};
  pointer-events: none;

  &:hover {
    box-shadow: 0 0 0 2px ${theme.palette.primary};
  }
`;

export const circle = ({ theme }) => css`
  width: 40px;
  height: 40px;
  border-radius: 999em;
  background-color: ${theme.palette.grey[500]};
  display: flex;
  align-items: center;
  justify-content: center;
  margin: ${theme.spacing(2)}px;
  float: left;
`;

export const title = ({ theme }) => css`
  ${theme.typography.h5}
  color:${theme.palette.text.primary};
  margin-top: ${theme.spacing(2)}px;
`;

export const type = ({ theme }) => css`
  ${theme.typography.body2}
  color:${theme.palette.text.secondary};
`;

export const status = ({ theme }) => css`
  border-top: 1px ${theme.palette.divider} solid;
  ${theme.typography.body2};
  width: 100%;
  float: left;
  padding: 4px ${theme.spacing(2)}px 0 ${theme.spacing(2)}px;
`;

export const left = css`
  float: left;
`;

export const right = css`
  float: right;
`;

export const menu = ({ theme }) => css`
  width: 24px;
  position: absolute;
  top: ${theme.spacing(1)}px;
  left: 245px;

  svg {
    color: ${theme.palette.grey[500]};
  }
`;
