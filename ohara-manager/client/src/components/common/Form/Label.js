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

import { lightBlue } from 'theme/variables';

const LabelWrapper = styled.label`
  color: ${lightBlue};
  width: ${({ width }) => width};
  font-size: 13px;
  margin: ${({ margin }) => margin};
`;

LabelWrapper.displayName = 'Label';

const Label = ({
  children,
  css = { margin: '0 0 8px', width: 'auto' },
  ...rest
}) => {
  return (
    <LabelWrapper {...css} {...rest}>
      {children}
    </LabelWrapper>
  );
};

Label.propTypes = {
  children: PropTypes.any,
  css: PropTypes.object,
};

export default Label;
