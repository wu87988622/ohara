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

import { Button } from 'common/Mui/Form';

export const FormInner = styled.div`
  padding: 20px;
`;

export const Table = styled.table`
  width: 100%;
  border-collapse: collapse;

  th,
  td {
    text-align: center;
    font-size: 13px;
    padding: 20px 10px;
    border-bottom: 1px solid ${props => props.theme.lighterGray};
  }
`;

export const NewRowBtn = styled(Button)`
  margin-left: auto;
`;

export const StyledTable = styled.table`
  width: 100%;
  border-collapse: collapse;

  th,
  td {
    text-align: center;
    font-size: 13px;
    padding: 20px 10px;
    border-bottom: 1px solid ${props => props.theme.lighterGray};
  }
`;
