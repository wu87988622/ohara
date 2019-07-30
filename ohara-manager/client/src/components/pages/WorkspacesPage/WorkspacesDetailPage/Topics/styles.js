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
import { Button } from 'components/common/Mui/Form';

export const ActionIcon = styled(Icon)`
  font-size: 20px;
`;

export const Main = styled.div`
  clear: both;
  padding: 30px 0;
  width: calc(100% - 60px);
  margin: auto;
`;

export const NewButton = styled(Button)`
  margin: 30px 30px 0;
  float: right;
`;
