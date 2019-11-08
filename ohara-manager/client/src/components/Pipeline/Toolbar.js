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
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import ButtonGroup from '@material-ui/core/ButtonGroup';
import StorageIcon from '@material-ui/icons/Storage';
import WavesIcon from '@material-ui/icons/Waves';
import FlightTakeoffIcon from '@material-ui/icons/FlightTakeoff';
import FlightLandIcon from '@material-ui/icons/FlightLand';
import Typography from '@material-ui/core/Typography';

import { Button } from 'components/common/Form';

const StyledToolbar = styled.div(
  ({ theme }) => css`
    height: 72px;
    padding: 0 ${theme.spacing(3)}px;
    background-color: #f5f6fa;
    border-bottom: 1px solid ${theme.palette.grey[100]};
    display: flex;
    align-items: center;

    .toolbox-toggle {
      display: flex;
      flex-direction: column;
      align-items: center;
    }
  `,
);

const Toolbar = props => {
  const { handleToolboxOpen, handleToolboxClick, isToolboxOpen } = props;

  const handleClick = panel => {
    // If the Toolbox is not "open", we should open it before opening
    // the expension panel
    if (!isToolboxOpen) handleToolboxOpen();
    handleToolboxClick(panel);
  };

  return (
    <StyledToolbar>
      <div className="toolbox-toggle">
        <ButtonGroup size="small">
          <Button onClick={() => handleClick('source')}>
            <FlightTakeoffIcon />
          </Button>
          <Button onClick={() => handleClick('topic')}>
            <StorageIcon />
          </Button>
          <Button onClick={() => handleClick('streamApp')}>
            <WavesIcon />
          </Button>
          <Button onClick={() => handleClick('sink')}>
            <FlightLandIcon />
          </Button>
        </ButtonGroup>
        <Typography variant="body2">Insert</Typography>
      </div>
    </StyledToolbar>
  );
};

Toolbar.propTypes = {
  handleToolboxOpen: PropTypes.func.isRequired,
  handleToolboxClick: PropTypes.func.isRequired,
  isToolboxOpen: PropTypes.bool.isRequired,
};

export default Toolbar;
