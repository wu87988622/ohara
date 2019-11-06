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
import Draggable from 'react-draggable';
import Typography from '@material-ui/core/Typography';
import InputBase from '@material-ui/core/InputBase';
import IconButton from '@material-ui/core/IconButton';
import SearchIcon from '@material-ui/icons/Search';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import AddIcon from '@material-ui/icons/Add';
import CloseIcon from '@material-ui/icons/Close';

import { StyledToolbox } from './Styles';

const Toolbox = props => {
  const { isOpen, expanded, handleClose, handleClick } = props;

  return (
    <Draggable bounds="parent">
      <StyledToolbox className={`${isOpen ? 'is-open' : ''}`}>
        <div className="title box-title">
          <Typography variant="subtitle1">Toolbox</Typography>
          <IconButton onClick={handleClose}>
            <CloseIcon />
          </IconButton>
        </div>
        <IconButton>
          <SearchIcon />
        </IconButton>
        <InputBase placeholder="Search topic & connector..." />

        <ExpansionPanel square expanded={expanded.topic}>
          <ExpansionPanelSummary
            className="panel-title"
            expandIcon={<ExpandMoreIcon />}
            onClick={() => handleClick('topic')}
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
        <ExpansionPanel square expanded={expanded.source}>
          <ExpansionPanelSummary
            className="panel-title"
            expandIcon={<ExpandMoreIcon />}
            onClick={() => handleClick('source')}
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
        <ExpansionPanel square expanded={expanded.streamApp}>
          <ExpansionPanelSummary
            className="panel-title"
            expandIcon={<ExpandMoreIcon />}
            onClick={() => handleClick('streamApp')}
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
        <ExpansionPanel square expanded={expanded.sink}>
          <ExpansionPanelSummary
            className="panel-title"
            expandIcon={<ExpandMoreIcon />}
            onClick={() => handleClick('sink')}
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
      </StyledToolbox>
    </Draggable>
  );
};

Toolbox.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  handleClose: PropTypes.func.isRequired,
  handleClick: PropTypes.func.isRequired,
  expanded: PropTypes.shape({
    topic: PropTypes.bool.isRequired,
    source: PropTypes.bool.isRequired,
    sink: PropTypes.bool.isRequired,
    streamApp: PropTypes.bool.isRequired,
  }).isRequired,
};

export default Toolbox;
