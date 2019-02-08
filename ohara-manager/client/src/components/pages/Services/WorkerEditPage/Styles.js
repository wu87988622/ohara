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
import { FormGroup } from 'common/Form';

const TopWrapper = styled.div`
  margin-bottom: 20px;
  display: flex;
  align-items: center;
`;

const Text = styled.div`
  width: ${props => props.width || '100%'};
  border: 1px solid ${props => props.theme.lighterGray};
  padding: 0.5rem 1rem;
  border-radius: 0.25rem;
  font-size: 12px;
  cursor: not-allowed;
`;

const List = styled.div`
  position: relative;
  width: ${props => props.width || '100%'};
  border: 1px solid ${props => props.theme.lighterGray};
  padding: 0.5rem 1rem;
  border-radius: 0.25rem;
  min-height: 8rem;
  cursor: not-allowed;
`;

const ListItem = styled.div`
  color: ${props => props.theme.lightBlue};
  margin: 0.25rem 0;
  font-size: 13px;
`;

const FormRow = styled(FormGroup).attrs({
  isInline: true,
})``;

const FormCol = styled(FormGroup)`
  width: ${props => props.width || '100%'};
`;

export { TopWrapper, List, ListItem, FormRow, FormCol, Text };
