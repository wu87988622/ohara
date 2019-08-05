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
import Icon from '@material-ui/core/Icon';
import { Link } from 'react-router-dom';

import { Button } from 'components/common/Mui/Form';
import { Table } from 'components/common/Mui/Table';

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

export const NewNodeBtn = styled(Button)`
  margin: 0 0 0 auto;
`;

export const TestConnectionBtn = styled(Button)`
  margin-right: auto;
`;

export const NodeTable = styled(Table)`
  text-align: left;
`;

export const LinkIcon = styled(Link)`
  color: ${props => props.theme.lightBlue};

  &:hover {
    color: ${props => props.theme.blue};
  }
`;

export const StyledIcon = styled(Icon)`
  font-size: 20px;
`;
