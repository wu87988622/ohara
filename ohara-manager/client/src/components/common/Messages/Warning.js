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

import {
  lighterBlue,
  lightYellow,
  lightOrange,
  radiusCompact,
} from 'theme/variables';

const WarningWrapper = styled.p`
  font-size: 13px;
  margin: 0 0 8px 0;
  color: ${lighterBlue};
  display: flex;
  align-items: center;
`;

const IconWrapper = styled.i`
  padding: 5px 10px;
  background-color: ${lightYellow};
  margin-right: 10px;
  display: inline-block;
  color: ${lightOrange};
  font-size: 12px;
  border-radius: ${radiusCompact};
  align-self: flex-start;
`;

const Warning = ({ text }) => {
  return (
    <WarningWrapper>
      <IconWrapper className="fas fa-exclamation" />
      <span>{text}</span>
    </WarningWrapper>
  );
};

Warning.propTypes = {
  text: PropTypes.string.isRequired,
};

export default Warning;
