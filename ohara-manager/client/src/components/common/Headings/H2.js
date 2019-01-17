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

const H2Wrapper = styled.h2`
  font-weight: normal;
  font-size: 24px;
`;

H2Wrapper.displayName = 'H2';

const H2 = ({ children, ...rest }) => {
  return <H2Wrapper {...rest}>{children}</H2Wrapper>;
};

H2.propTypes = {
  children: PropTypes.any,
};

export default H2;
