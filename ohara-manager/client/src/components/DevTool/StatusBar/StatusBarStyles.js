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

export const StyledStatusBar = styled.div(
  ({ theme }) => css`
    width: 100%;
    line-height: 26px;
    padding-left: ${theme.spacing(1)}px;
    height: 26px;
    z-index: ${theme.zIndex.appBar};
    background-color: ${props => props.theme.palette.grey[50]};
    color: ${props => props.theme.palette.text.disabled};
    border: 1px;
    border-color: ${theme.palette.grey[200]};
    border-style: solid;
  `,
);
