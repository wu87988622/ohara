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

import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const FormGroupWrapper = styled.div`
  display: flex;
  flex-direction: ${({ isInline }) => (isInline ? 'row' : 'column')};
  align-items: ${({ isInline }) => (isInline ? 'center' : 'flex-start')};
  width: ${({ width }) => width};
  height: ${({ height }) => height};
  margin: ${({ margin }) => margin};
`;

FormGroupWrapper.displayName = 'FormGroup';

const FormGroup = ({
  children,
  css = {
    width: 'auto',
    height: 'auto',
    margin: '0 0 20px',
  },
  isInline = false,
  ...rest
}) => {
  return (
    <FormGroupWrapper isInline={isInline} {...css} {...rest}>
      {children}
    </FormGroupWrapper>
  );
};

FormGroup.propTypes = {
  children: PropTypes.any.isRequired,
  css: PropTypes.object,
  isInline: PropTypes.bool,
};

export default FormGroup;
