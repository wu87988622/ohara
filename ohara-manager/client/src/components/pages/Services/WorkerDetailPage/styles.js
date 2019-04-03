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

const Item = styled.div`
  display: flex;
  margin-bottom: 25px;

  h5 {
    white-space: nowrap;
    margin: 0 10px 0 0;
    padding: 13px 15px;
    color: ${props => props.theme.darkerBlue};
    background-color: ${props => props.theme.whiteSmoke};
    align-self: start;
  }

  .content {
    font-size: 13px;
    color: ${props => props.theme.lightBlue};
    align-self: center;
  }
`;

export { Item };
