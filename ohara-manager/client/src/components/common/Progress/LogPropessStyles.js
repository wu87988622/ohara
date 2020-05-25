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

import { omit } from 'lodash';
import React from 'react';
import styled, { css } from 'styled-components';
import Dialog from '@material-ui/core/Dialog';

const StyledDialog = styled(props => (
  // Don't pass down props that are meant only used by styled-component.
  // React also complains about this if these props are not omitted
  <Dialog {...omit(props, ['isHidden', 'isDeleteDialog'])} />
))(
  ({ isHidden, isDeleteDialog }) => css`
    .MuiDialog-paperFullWidth {
      height: ${isHidden ? '' : '600px'};
    }

    .FlexDiv {
      display: flex;
      flex-wrap: ${isHidden ? 'nowrap' : 'wrap'};
    }
    .FlexIconButtonDiv {
      display: flex;
      justify-content: flex-end;
    }
    .FlexFooterDiv {
      display: ${isDeleteDialog ? 'none' : 'flex'};
    }
    .StyledProgress {
      flex: ${isHidden ? 1 : '100%'};
      &.MuiLinearProgress-root {
        height: 12px;
        margin-left: 12px;
        margin-right: 12px;
      }
    }
    .RightFlexDiv {
      display: flex;
      flex: ${isHidden ? 0 : 1};
      justify-content: ${isHidden ? 'flex-end' : 'center'};
      margin-right: ${isHidden ? '12px' : '0px'};
    }
    .SuspendButton {
      top: ${isHidden ? '-12px' : '0px'};
    }
    .StyledIconButton {
      margin-right: 12px;
    }
    .StyledCard {
      margin-left: 12px;
      margin-right: 12px;
      min-height: 345px;
    }
    .StyledFormControl {
      margin-left: 12px;
      justify-content: flex-start;
      flex: 1;
    }
    .StyledCloseButton {
      justify-content: flex-end;
      margin-right: 12px;
    }
    .StyledTypography {
      justify-content: flex-start;
      flex: 1;
      margin-left: 12px;
    }
  `,
);

export { StyledDialog };
