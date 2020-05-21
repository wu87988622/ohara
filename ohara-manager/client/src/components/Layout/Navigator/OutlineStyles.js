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
  ({ theme, isExpanded }) => css`
    /* 
       Since pipeline list is affecting outline height, we have to calculate
       this when it's expanded or collapsed

       58px -> outline heading height
       108px -> pipeline heading (48px) + workspace name (60px)
     */

    height: ${isExpanded ? 'calc(50% - 58px)' : 'calc(100% - 108px)'};

    .scrollbar-wrapper {
      height: ${isExpanded ? '100%' : 'calc(100% - 58px)'};

      /* 
        TODO: Override react-scrollbars-custom's default styles with props or custom styles
       */

      .ScrollbarsCustom-TrackY {
        top: 0 !important;
        height: 100% !important;
        width: 5px !important;
      }
    }

    h5 {
      padding: 0 ${theme.spacing(2)}px;
      color: ${theme.palette.common.white};
      padding: ${theme.spacing(2)}px;
      border-top: 1px solid ${theme.palette.primary[400]};
      border-bottom: 1px solid ${theme.palette.primary[400]};
      background-color: ${theme.palette.primary[500]};

      display: flex;
      align-items: center;

      .MuiSvgIcon-root {
        margin-right: ${theme.spacing(1)}px;
      }
    }

    li {
      color: ${theme.palette.common.white};
      display: flex;
      align-items: center;
      color: ${theme.palette.common.white};
      line-height: 36px;
      font-size: 14px;
      padding: ${theme.spacing(0, 3)};
      cursor: pointer;

      &:hover,
      &.is-selected {
        background-color: rgba(255, 255, 255, 0.3);
      }

      /* The custom icon needs some more padding in order to be aligned 
        with the rest of items
      */
      &.is-shared {
        padding-left: 26px;
      }

      > svg {
        margin-right: ${theme.spacing(1)}px;
        width: 20px;
      }
    }
  `,
);
