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
import { H2, H3 } from 'common/Headings';

const Wrapper = styled.div`
  padding-top: 75px;
  max-width: 1200px;
  width: calc(100% - 100px);
  margin: auto;
`;

Wrapper.displayName = 'Wrapper';

const Main = styled.div`
  display: flex;
`;

const Sidebar = styled.div`
  width: 35%;
`;

const Heading2 = styled(H2)`
  font-size: 16px;
  color: ${props => props.theme.lightBlue};
`;

Heading2.displayName = 'H2';

const Heading3 = styled(H3)`
  font-size: 15px;
  font-weight: normal;
  margin: 0;
  color: ${props => props.theme.lightBlue};
`;

Heading3.displayName = 'H3';

const Operate = styled.div`
  .actions {
    display: flex;
    align-items: center;
    margin-bottom: 10px;
  }

  .action-btns {
    margin-left: 10px;

    button {
      color: ${props => props.theme.dimBlue};
      padding: 0 4px;
      border: 0;
      font-size: 20px;
      cursor: pointer;
      background-color: transparent;
      transition: ${props => props.theme.durationNormal} all;

      &:hover {
        color: ${props => props.theme.blue};
        transition: ${props => props.theme.durationNormal} all;
      }

      &.stop-btn:hover {
        color: ${props => props.theme.red};
      }
    }
  }

  .cluster-name {
    display: block;
    font-size: 12px;
    color: ${props => props.theme.lighterBlue};
  }
`;

export { Wrapper, Main, Sidebar, Heading2, Heading3, Operate };
