import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import {
  lighterBlue,
  lightYellow,
  lightOrange,
  radiusCompact,
} from '../../../theme/variables';

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
