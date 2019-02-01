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
import { NavLink } from 'react-router-dom';

import { Button } from 'common/Form';
import { DataTable } from 'common/Table';
import * as CSS_VARS from 'theme/variables';

const Wrapper = styled.div`
  padding-top: 75px;
  max-width: 1200px;
  width: calc(100% - 100px);
  margin: auto;
`;

const NewClusterBtn = styled(Button)`
  margin-left: auto;
`;

NewClusterBtn.displayName = 'NewClusterBtn';

const Table = styled(DataTable)`
  text-align: left;

  .is-running {
    background: ${CSS_VARS.trBgColor};
  }
`;

const Link = styled(NavLink)`
  color: ${CSS_VARS.lightBlue};

  &:hover {
    color: ${CSS_VARS.blue};
  }
`;

const TooltipWrapper = styled.div`
  margin-left: auto;
`;

const Icon = styled.i`
  color: ${CSS_VARS.lighterBlue};
  font-size: 20px;
  margin-right: 20px;
  transition: ${CSS_VARS.durationNormal} all;
  cursor: pointer;

  &:hover,
  &.is-active {
    transition: ${CSS_VARS.durationNormal} all;
    color: ${CSS_VARS.blue};
  }

  &:last-child {
    border-right: none;
    margin-right: 0;
  }
`;

export { Wrapper, NewClusterBtn, Table, Link, Icon, TooltipWrapper };
