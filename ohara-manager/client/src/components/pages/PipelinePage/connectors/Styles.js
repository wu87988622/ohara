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
import * as CSS_VARS from 'theme/variables';

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
  color: ${CSS_VARS.lightBlue};
`;

H5Wrapper.displayName = 'H5';

const Controller = styled.div`
  position: absolute;
  right: 0;
`;

const ControlButton = styled.button`
  color: ${CSS_VARS.lightBlue};
  border: 0;
  font-size: 20px;
  cursor: pointer;
  background-color: transparent;

  &:hover {
    color: blue;
  }
`;

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
  color: ${CSS_VARS.lighterBlue};
`;

const ViewTopologyBtn = styled(Button)`
  margin-right: auto;
`;

export {
  BoxWrapper,
  TitleWrapper,
  H5Wrapper,
  Controller,
  ControlButton,
  FormRow,
  FormCol,
  JarNameText,
  ViewTopologyBtn,
};
