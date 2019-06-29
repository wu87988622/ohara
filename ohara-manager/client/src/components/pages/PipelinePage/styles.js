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

import { DataTable } from 'components/common/Table';
import { Button } from 'components/common/Form';

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

export const NewPipelineBtn = styled(Button)`
  margin-left: auto;
`;

NewPipelineBtn.displayName = 'NewPipelineBtn';

export const Table = styled(DataTable)`
  text-align: center;

  .is-running {
    background: ${props => props.theme.trBgColor};
  }
`;

Table.displayName = 'Table';

export const LinkIcon = styled(Link)`
  color: ${props => props.theme.lightBlue};

  &:hover {
    color: ${props => props.theme.blue};
  }
`;

LinkIcon.displayName = 'LinkIcon';

export const DeleteIcon = styled.button`
  color: ${props => props.theme.lightBlue};
  border: 0;
  font-size: 20px;
  cursor: pointer;
  background-color: transparent;

  &:hover {
    color: ${props => props.theme.red};
  }
`;

DeleteIcon.displayName = 'DeleteIcon';

export const Inner = styled.div`
  padding: 30px 20px;
`;

export const LoaderWrapper = styled.div`
  margin: 30px;
`;
