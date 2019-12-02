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
    padding: ${theme.spacing(4)}px;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100vh;
    width: 100vw;
    flex-direction: column;
    background-color: ${theme.palette.grey[100]};

    > svg {
      margin: ${theme.spacing(2)}px;
    }

    /* 501 page specific */
    .current-version-section,
    .suggestion-section {
      margin-bottom: ${theme.spacing(6)}px;

      h3 {
        margin-bottom: ${theme.spacing(2)}px;
      }

      ul {
        text-align: center;

        li:not(:last-child) {
          margin-bottom: ${theme.spacing(1)}px;
        }
      }
    }

    .suggestion-section {
      margin-bottom: ${theme.spacing(2)}px;

      h5 {
        width: 500px;
        text-align: center;
      }
    }

    /* Loader */
    .MuiCircularProgress-root {
      margin-top: ${theme.spacing(2)}px;
    }
  `,
);
