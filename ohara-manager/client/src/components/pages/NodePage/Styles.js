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
import { Link } from 'react-router-dom';

import { Button } from 'common/Form';
import MuiButton from '@material-ui/core/Button';
import MuiTable from '@material-ui/core/Table';

export const Wrapper = styled.div`
  padding-top: 75px;
  max-width: 1200px;
  width: calc(100% - 100px);
  margin: auto;
`;

export const TopWrapper = styled.div`
  margin-bottom: 20px;
  display: flex;
  align-items: center;
`;

export const MuiBtn = styled(MuiButton)`
  margin-left: auto;
`;

export const TestConnectionBtn = styled(Button)`
  margin-right: auto;
`;

export const Table = styled(MuiTable)`
  text-align: left;
  .is-running {
    background: ${props => props.theme.trBgColor};
  }
`;

export const LinkIcon = styled(Link)`
  color: ${props => props.theme.lightBlue};

  &:hover {
    color: ${props => props.theme.blue};
  }
`;

export const Icon = styled.i`
  color: ${props => props.theme.lighterBlue};
  font-size: 20px;
  margin-right: 20px;
  transition: ${props => props.theme.durationNormal} all;
  cursor: pointer;

  &:hover,
  &.is-active {
    transition: ${props => props.theme.durationNormal} all;
    color: ${props => props.theme.blue};
  }

  &:last-child {
    border-right: none;
    margin-right: 0;
  }
`;
