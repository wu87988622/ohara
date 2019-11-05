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

import React, { useState } from 'react';
import styled from 'styled-components';
import Draggable from 'react-draggable'; // The default
import Typography from '@material-ui/core/Typography';
import InputBase from '@material-ui/core/InputBase';
import IconButton from '@material-ui/core/IconButton';
import SearchIcon from '@material-ui/icons/Search';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import AddIcon from '@material-ui/icons/Add';

const StyledToolbox = styled.div`
  position: absolute;
  top: ${props => props.theme.spacing(1)}px;
  left: ${props => props.theme.spacing(1)}px;
  width: 272px;
  background-color: ${props => props.theme.palette.common.white};
  box-shadow: ${props => props.theme.shadows[24]};

  .title {
    padding: ${props => props.theme.spacing(1)}px
      ${props => props.theme.spacing(2)}px;
    background-color: #f5f6fa;

    &.box-title {
      cursor: move;
    }
  }

  .add-button {
    display: flex;
    align-items: center;
    button {
      margin-right: ${props => props.theme.spacing(1)}px;
    }
  }

  .detail {
    padding: ${props => props.theme.spacing(1)}px
      ${props => props.theme.spacing(2)}px;
  }

  .panel-title {
    background-color: #f5f6fa;
  }

  .MuiExpansionPanel-root {
    box-shadow: none;
  }

  .MuiExpansionPanelSummary-root.Mui-expanded {
    min-height: 48px;
  }
  .MuiExpansionPanelSummary-content.Mui-expanded,
  .MuiExpansionPanel-root.Mui-expanded {
    margin: 0;
  }

  .MuiExpansionPanel-root:before {
    opacity: 1;
  }

  .MuiExpansionPanel-root.Mui-expanded:before {
    opacity: 1;
  }
`;

const Toolbox = () => {
  const [expanded, setExpanded] = useState('topic-panel');

  const handleChange = panel => (event, newExpanded) => {
    setExpanded(newExpanded ? panel : false);
  };

  return (
    <Draggable bounds="parent">
      <StyledToolbox>
        <div className="title box-title">
          <Typography variant="subtitle1">Toolbox</Typography>
        </div>
        <IconButton>
          <SearchIcon />
        </IconButton>
        <InputBase placeholder="Search topic & connector..." />

        <ExpansionPanel
          square
          expanded={expanded === 'topic-panel'}
          onChange={handleChange('topic-panel')}
        >
          <ExpansionPanelSummary
            className="panel-title"
            expandIcon={<ExpandMoreIcon />}
          >
            <Typography variant="subtitle1">Topic</Typography>
          </ExpansionPanelSummary>
          <ExpansionPanelDetails className="detail">
            <div className="add-button">
              <IconButton>
                <AddIcon />
              </IconButton>
              <Typography variant="subtitle2">Add topics</Typography>
            </div>
          </ExpansionPanelDetails>
        </ExpansionPanel>
        <ExpansionPanel
          square
          expanded={expanded === 'source-panel'}
          onChange={handleChange('source-panel')}
        >
          <ExpansionPanelSummary
            className="panel-title"
            expandIcon={<ExpandMoreIcon />}
          >
            <Typography variant="subtitle1">Source</Typography>
          </ExpansionPanelSummary>
          <ExpansionPanelDetails className="detail">
            <div className="add-button">
              <IconButton>
                <AddIcon />
              </IconButton>
              <Typography variant="subtitle2">Add source connectors</Typography>
            </div>
          </ExpansionPanelDetails>
        </ExpansionPanel>
        <ExpansionPanel
          square
          expanded={expanded === 'sink-panel'}
          onChange={handleChange('sink-panel')}
        >
          <ExpansionPanelSummary
            className="panel-title"
            expandIcon={<ExpandMoreIcon />}
          >
            <Typography variant="subtitle1">Sink</Typography>
          </ExpansionPanelSummary>
          <ExpansionPanelDetails className="detail">
            <div className="add-button">
              <IconButton>
                <AddIcon />
              </IconButton>
              <Typography variant="subtitle2">Add sink connectors</Typography>
            </div>
          </ExpansionPanelDetails>
        </ExpansionPanel>
        <ExpansionPanel
          square
          expanded={expanded === 'streamapp-panel'}
          onChange={handleChange('streamapp-panel')}
        >
          <ExpansionPanelSummary
            className="panel-title"
            expandIcon={<ExpandMoreIcon />}
          >
            <Typography variant="subtitle1">StreamApp</Typography>
          </ExpansionPanelSummary>
          <ExpansionPanelDetails className="detail">
            <div className="add-button">
              <IconButton>
                <AddIcon />
              </IconButton>
              <Typography variant="subtitle2">Add stream apps</Typography>
            </div>
          </ExpansionPanelDetails>
        </ExpansionPanel>
      </StyledToolbox>
    </Draggable>
  );
};

export default Toolbox;
