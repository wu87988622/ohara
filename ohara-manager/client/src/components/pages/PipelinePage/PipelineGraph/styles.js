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

import styled from 'styled-components';

import { Box } from 'common/Layout';
import { H5 } from 'common/Headings';

const Wrapper = styled(Box)`
  width: 65%;
  margin-right: 20px;
  min-height: 800px;
`;

Wrapper.displayName = 'Box';

const H5Wrapper = styled(H5)`
  margin: 0 0 30px;
  font-weight: normal;
  color: ${props => props.theme.lightBlue};
`;

H5Wrapper.displayName = 'H5Wrapper';

const Svg = styled.svg`
  width: 100%;
  height: 100%;

  .node {
    circle,
    rect {
      fill: transparent;
      cursor: pointer;
      border: 1px solid ${props => props.theme.whiteSmoke};
    }
    foreignObject {
      /* Make topic name visible */
      overflow: visible;
    }
  }

  .node-graph {
    cursor: pointer;
  }

  .node-name {
    font-size: 14px;
    color: ${props => props.theme.lightBlue};
  }

  .node-topic {
    position: relative;
    width: 60px;
    height: 60px;
    display: flex;
    justify-content: center;
    align-items: center;
    border: 1px solid ${props => props.theme.lighterGray};
    border-radius: ${props => props.theme.radiusRounded};
    box-shadow: ${props => props.theme.shadowNormal};

    .node-text-wrapper {
      position: absolute;
      top: calc(100% + 10px);
    }

    /* These labels are not needed in topics */
    .node-type,
    .node-status {
      display: none;
    }

    .node-icon {
      color: ${props => props.theme.dimBlue};
    }
    .status-icon {
      display: none;
    }
  }

  .node-connector {
    width: 200px;
    min-height: 90px;
    padding: 15px 20px;
    border: 1px solid ${props => props.theme.lighterGray};
    border-radius: ${props => props.theme.radiusNormal};
    box-shadow: ${props => props.theme.shadowNormal};
    display: flex;

    .node-icon {
      display: flex;
      justify-content: center;
      align-items: center;
      width: 40px;
      height: 40px;
      margin-right: 8px;
      color: ${props => props.theme.white};
      border-radius: ${props => props.theme.radiusRounded};
      background-color: ${props => props.theme.lightestBlue};
    }

    .node-text-wrapper {
      display: flex;
      flex-direction: column;
      color: ${props => props.theme.dimBlue};
    }

    .node-name {
      margin-bottom: 5px;
      width: 110px;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .node-status {
      font-size: 11px;
      color: ${props => props.theme.lighterBlue};
      margin-bottom: 5px;
    }

    .node-type {
      font-size: 11px;
      width: 100px;
      white-space: nowrap;
      text-overflow: ellipsis;
      overflow: hidden;
    }

    .status-icon {
      position: absolute;
      width: 16px;
      height: 16px;
      right: 8px;
      top: 7px;
      display: none;
    }

    &.is-running {
      .node-icon {
        background-color: ${props => props.theme.green};
      }
    }

    &.is-failed {
      .node-icon {
        background-color: ${props => props.theme.red};
      }
      .status-icon {
        color: ${props => props.theme.red};
        display: block;
      }
    }
  }

  .fa {
    font-size: 16px;
  }

  path {
    stroke: ${props => props.theme.lighterGray};
    fill: ${props => props.theme.lighterGray};
    stroke-width: 2px;
  }
`;

Svg.displayName = 'Svg';

export { Wrapper, H5Wrapper, Svg };
