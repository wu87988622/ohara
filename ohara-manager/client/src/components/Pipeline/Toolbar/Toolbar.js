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
import ButtonGroup from '@material-ui/core/ButtonGroup';
import Typography from '@material-ui/core/Typography';
import StorageIcon from '@material-ui/icons/Storage';
import WavesIcon from '@material-ui/icons/Waves';
import FlightTakeoffIcon from '@material-ui/icons/FlightTakeoff';
import FlightLandIcon from '@material-ui/icons/FlightLand';
import FullscreenIcon from '@material-ui/icons/Fullscreen';
import FullscreenExitIcon from '@material-ui/icons/FullscreenExit';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
import AddIcon from '@material-ui/icons/Add';
import RemoveIcon from '@material-ui/icons/Remove';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Switch from '@material-ui/core/Switch';

import { StyledToolbar } from './ToolbarStyles';
import { Button } from 'components/common/Form';

const Toolbar = props => {
  const { handleToolboxOpen, handleToolboxClick, isToolboxOpen } = props;
  const [pipelineAnchorEl, setPipelineAnchorEl] = React.useState(null);
  const [zoomAnchorEl, setZoomAnchorEl] = React.useState(null);
  const [isMetricsDisplayed, setIsMetricsDisplayed] = React.useState(true);

  const handleZoomClick = event => {
    setZoomAnchorEl(event.currentTarget);
  };

  const handleZoomClose = () => {
    setZoomAnchorEl(null);
  };

  const handlePipelineControlsClick = event => {
    setPipelineAnchorEl(event.currentTarget);
  };

  const handlePipelineControlsClose = () => {
    setPipelineAnchorEl(null);
  };

  const onToolboxClick = panel => {
    // If the Toolbox is not "open", we should open it before opening
    // the expansion panel
    if (!isToolboxOpen) handleToolboxOpen();
    handleToolboxClick(panel);
  };

  return (
    <StyledToolbar>
      <div className="toolbox-controls">
        <ButtonGroup size="small">
          <Button onClick={() => onToolboxClick('source')}>
            <FlightTakeoffIcon color="action" />
          </Button>
          <Button onClick={() => onToolboxClick('topic')}>
            <StorageIcon color="action" />
          </Button>
          <Button onClick={() => onToolboxClick('stream')}>
            <WavesIcon color="action" />
          </Button>
          <Button onClick={() => onToolboxClick('sink')}>
            <FlightLandIcon color="action" />
          </Button>
        </ButtonGroup>
        <Typography variant="body2">Insert</Typography>
      </div>
      <div className="paper-controls">
        <div className="zoom">
          <ButtonGroup size="small">
            <Button>
              <RemoveIcon color="action" />
            </Button>
            <Button
              onClick={handleZoomClick}
              variant="outlined"
              color="default"
              size="small"
            >
              100%
            </Button>
            <Button>
              <AddIcon color="action" />
            </Button>
          </ButtonGroup>
          <Typography variant="body2">Zoom</Typography>

          <Menu
            anchorEl={zoomAnchorEl}
            keepMounted
            open={Boolean(zoomAnchorEl)}
            onClose={handleZoomClose}
          >
            <MenuItem onClick={handleZoomClose}>50%</MenuItem>
            <MenuItem onClick={handleZoomClose}>100%</MenuItem>
            <MenuItem onClick={handleZoomClose}>200%</MenuItem>
          </Menu>
        </div>

        <div className="fit">
          <Button variant="outlined" color="default" size="small">
            <FullscreenIcon color="action" />
          </Button>
          <Typography variant="body2">Fit</Typography>
        </div>

        <div className="center">
          <Button variant="outlined" color="default" size="small">
            <FullscreenExitIcon color="action" />
          </Button>
          <Typography variant="body2">Center</Typography>
        </div>
      </div>

      <div className="pipeline-controls">
        <Button
          onClick={handlePipelineControlsClick}
          variant="outlined"
          color="default"
          size="small"
          endIcon={<ArrowDropDownIcon />}
        >
          PIPELINE
        </Button>
        <Typography variant="body2">Actions</Typography>

        <Menu
          anchorEl={pipelineAnchorEl}
          keepMounted
          open={Boolean(pipelineAnchorEl)}
          onClose={handlePipelineControlsClose}
        >
          <MenuItem onClick={handlePipelineControlsClose}>
            Start all components
          </MenuItem>
          <MenuItem onClick={handlePipelineControlsClose}>
            Stop all components
          </MenuItem>
          <MenuItem onClick={handlePipelineControlsClose}>
            Delete this pipeline
          </MenuItem>
        </Menu>
      </div>

      <div className="metrics-controls">
        <FormControlLabel
          control={
            <Switch
              checked={isMetricsDisplayed}
              onChange={() => setIsMetricsDisplayed(prevState => !prevState)}
              color="primary"
            />
          }
        />

        <Typography variant="body2">Metrics</Typography>
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
