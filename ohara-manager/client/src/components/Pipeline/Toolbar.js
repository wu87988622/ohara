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
import styled from 'styled-components';
import ButtonGroup from '@material-ui/core/ButtonGroup';
import IconButton from '@material-ui/core/IconButton';
import DnsIcon from '@material-ui/icons/Dns';
import WavesIcon from '@material-ui/icons/Waves';
import FlightTakeoffIcon from '@material-ui/icons/FlightTakeoff';
import FlightLandIcon from '@material-ui/icons/FlightLand';
import Typography from '@material-ui/core/Typography';

const StyledToolbar = styled.div`
  height: 72px;
  padding: 0 ${props => props.theme.spacing(3)}px;
  background-color: #f5f6fa;
  border-bottom: 1px solid ${props => props.theme.palette.grey[300]};
  display: flex;
  align-items: center;

  .toolbox-toggle {
    display: flex;
    flex-direction: column;
    align-items: center;
  }
`;

const Toolbar = () => {
  return (
    <StyledToolbar>
      <div className="toolbox-toggle">
        <ButtonGroup variant="contained" color="default" size="small">
          <IconButton>
            <FlightTakeoffIcon />
          </IconButton>
          <IconButton>
            <DnsIcon />
          </IconButton>
          <IconButton>
            <WavesIcon />
          </IconButton>
          <IconButton>
            <FlightLandIcon />
          </IconButton>
        </ButtonGroup>

        <Typography variant="body2">Insert</Typography>
      </div>
    </StyledToolbar>
  );
};

export default Toolbar;
