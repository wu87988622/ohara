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
import { TableRow } from '@material-ui/core';

export const DevTool = styled.div(
  ({ theme }) => css`
    position: absolute;
    left: 70px;
    width: calc(100% - 70px);
    height: 468px;
    bottom: 0;
    z-index: 999;

    .header {
      display: flex;
      flex-direction: row;
      justify-content: space-between;
      position: absolute;
      width: 100%;
      height: 48px;
      background-color: ${theme.palette.common.white};
    }

    .items {
      display: flex;
      flex-direction: row;
      justify-content: center;
      margin: ${theme.spacing(1)}px;
      color: ${theme.palette.grey[500]};
      align-items: center;
      cursor: pointer;

      .input {
        min-width: 200px;
        height: 28px;
        margin: 0;
      }

      .item {
        width: 24px;
        height: 24px;
        margin: 4px;

        &:hover {
          background-color: ${theme.palette.grey[200]};
        }
      }
    }

    .tab-body {
      flex-direction: row;
      position: absolute;
      width: 100%;
      height: calc(100% - 48px);
      overflow: auto;
      bottom: 0;
      background-color: ${theme.palette.common.white};
    }
  `,
);

export const DialogBody = styled.div`
  textarea {
    width: 100%;
    height: 200px;
    resize: none;
  }
`;

export const FooterRow = styled(TableRow)`
  td {
    padding: 1px;
    height: 26px;
  }
`;

export const SearchBody = styled.div`
  display: grid;
  width: 235px;
  height: 208px;
  background-color: ${props => props.theme.palette.common.white};
  padding: 20px;

  button {
    width: 88px;
    height: 36px;
    background-color: ${props => props.theme.palette.primary[600]};
  }
`;
