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

import React from 'react';
import { omit } from 'lodash';
import styled, { css } from 'styled-components';
import { FullScreenDialog } from 'components/common/Dialog';

export const StyledFullScreenDialog = styled(props => (
  // Don't pass down props that are meant only used by styled-component.
  // React also complains about this if these props are not omitted
  <FullScreenDialog {...omit(props, 'isPageComponent')} />
))(
  ({ theme, isPageComponent }) => css`
    .MuiDialogContent-root {
      max-width: none;
      width: auto;
      margin: 0;

      .dialog-inner {
        background-color: ${isPageComponent
          ? theme.palette.background.paper
          : 'none'};
        width: 680px;
        margin: auto;
        padding: ${theme.spacing(12, 3)};
        height: 100%;
      }

      @media (max-width: 1200px) {
        width: 1200px;
      }
    }
  `,
);

export const Wrapper = styled.div(
  () => css`
    display: flex;

    .settings-menu {
      position: fixed;
      top: 100px;
      left: 40px;
      height: 100%;
      width: 180px;
    }

    section {
      position: relative;
    }

    /* Offset used in scroll. We need to do this as the the dialog title height is added in the scrollIntoView API */
    .anchor-element {
      position: absolute;
      margin-top: -90px;
    }
  `,
);
