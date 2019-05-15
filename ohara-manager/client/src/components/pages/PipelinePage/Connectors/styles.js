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
import { Box } from 'common/Layout';
import { H5 } from 'common/Headings';
import { FormGroup, Button, Label } from 'common/Form';

const BoxWrapper = styled(Box)`
  padding: ${props => props.padding || '25px'};
`;

const TitleWrapper = styled(FormGroup).attrs({
  isInline: true,
})`
  position: relative;
  margin: ${props => props.margin || '0 0 30px'};
`;

const H5Wrapper = styled(H5)`
  margin: 0;
  font-weight: normal;
  color: ${props => props.theme.lightBlue};
`;

H5Wrapper.displayName = 'H5';

const FormRow = styled(FormGroup).attrs({
  isInline: true,
})`
  & > div:first-child {
    margin-right: 8px;
  }
  & > div:not(:first-child) {
    margin-left: 8px;
  }
`;

const FormCol = styled(FormGroup)`
  width: ${props => props.width || '100%'};
  margin: 0;
`;

const JarNameText = styled(Label)`
  font-size: 12px;
  color: ${props => props.theme.lighterBlue};
`;

const ViewTopologyBtn = styled(Button)`
  display: none;
  margin-right: auto;
`;

const StyledForm = styled.div`
  padding: 20px;
`;

export {
  BoxWrapper,
  TitleWrapper,
  H5Wrapper,
  FormRow,
  FormCol,
  JarNameText,
  ViewTopologyBtn,
  StyledForm,
};
