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
import { DataTable } from 'components/common/Table';

export const Icon = styled.i`
  color: ${props => props.theme.lighterBlue};
  font-size: 25px;
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

Icon.displayName = 'Icon';

export const TableWrapper = styled.div`
  margin: 30px 30px 40px;
`;

export const Table = styled(DataTable)`
  thead th {
    color: ${props => props.theme.lightBlue};
    font-weight: normal;
  }

  td {
    color: ${props => props.theme.lighterBlue};
  }

  tbody tr {
    cursor: pointer;
  }

  .is-active {
    background-color: ${props => props.theme.trBgColor};
  }
`;

export const Wrapper = styled.div`
  padding: 20px;
`;

export const ToolbarWrapper = styled.div`
  margin-bottom: 15px;
  padding: 15px 30px;
  border: 1px solid ${props => props.theme.lightestBlue};
  border-radius: ${props => props.theme.radiusNormal};
  display: flex;
  align-items: center;
`;

ToolbarWrapper.displayName = 'ToolbarWrapper';

export const FileSavingStatus = styled.div`
  margin-left: auto;
  color: red;
  font-size: 12px;
  color: ${props => props.theme.lighterBlue};
`;

FileSavingStatus.displayName = 'FileSavingStatus';

export const Inner = styled.div`
  padding: 30px 20px;
`;

export const LoaderWrapper = styled.div`
  margin: 20px 40px;
`;
