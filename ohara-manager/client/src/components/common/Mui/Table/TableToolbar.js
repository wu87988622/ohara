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

/* eslint-disable react/prop-types */
import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import Icon from '@material-ui/core/Icon';
import Button from '@material-ui/core/Button';
import Toolbar from '@material-ui/core/Toolbar';
import Typography from '@material-ui/core/Typography';

const Spacer = styled.div`
  flex: 1 1 100%;
`;

const StyledTypography = styled(Typography)`
  color: white;
  flex: 0 0 auto;
`;

const StyledToolbar = styled(Toolbar)`
  background-color: ${props => props.theme.palette.warning.main};
`;

const StyledButton = styled(Button)`
  color: white;
  margin-left: 20px;
`;

const ActionIcon = styled(Icon)`
  margin-right: 10px;
  color: white;
  width: 30px;
`;

const TableToolbar = props => {
  const { newCount, deleteCount, handleDiscard, handleRestart } = props;

  const message = () => {
    let message = 'You’ve made some changes to the plugins:';
    if (newCount > 0 && deleteCount > 0) {
      message += ` ${newCount} added and ${deleteCount}
      removed.`;
    } else if (newCount > 0) {
      message += ` ${newCount} added.`;
    } else {
      message += ` ${deleteCount} removed.`;
    }
    message += ' Please restart for these settings to take effect!!';
    return message;
  };

  return (
    <StyledToolbar>
      <ActionIcon className="fas fa-exclamation-triangle" />
      <StyledTypography>{message()}</StyledTypography>
      <Spacer />
      <StyledButton onClick={() => handleDiscard()}>DISCARD</StyledButton>
      <StyledButton onClick={() => handleRestart()}>
        <b>RESTART</b>
      </StyledButton>
    </StyledToolbar>
  );
};

TableToolbar.propTypes = {
  newCount: PropTypes.number,
  deleteCount: PropTypes.number,
  handleDiscard: PropTypes.func.isRequired,
  handleRestart: PropTypes.func.isRequired,
};

export default TableToolbar;
