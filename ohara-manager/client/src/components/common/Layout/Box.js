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

import { white, shadowNormal, radiusNormal } from '../../../theme/variables';

const BoxWrapper = styled.div`
  padding: 25px;
  background-color: ${white};
  box-shadow: ${props => (props.shadow ? shadowNormal : '')};
  border-radius: ${radiusNormal};
  margin-bottom: 20px;
`;

BoxWrapper.displayName = 'BoxWrapper';

const Box = ({ children, ...rest }) => {
  return <BoxWrapper {...rest}>{children}</BoxWrapper>;
};

Box.propTypes = {
  children: PropTypes.any,
};

Box.defaultProps = {
  shadow: true,
};

export default Box;
