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
import Tooltip from '@material-ui/core/Tooltip';
import { useHistory } from 'react-router-dom';

import { StyledToolbar } from './ToolbarStyles';
import { Button } from 'components/common/Form';
import * as context from 'context';

const Toolbar = props => {
  const {
    paperScale,
    handleToolboxOpen,
    handleToolbarClick,
    isToolboxOpen,
    handleZoom,
    handleFit,
    handleCenter,
    hasSelectedCell,
  } = props;

  const [pipelineAnchorEl, setPipelineAnchorEl] = React.useState(null);
  const [zoomAnchorEl, setZoomAnchorEl] = React.useState(null);
  const [isMetricsDisplayed, setIsMetricsDisplayed] = React.useState(true);

  const { currentPipeline, error } = context.usePipelineState();
  const { deletePipeline } = context.usePipelineActions();
  const { currentWorkspace } = context.useWorkspace();
  const history = useHistory();

  const handleZoomClick = event => {
    setZoomAnchorEl(event.currentTarget);
  };

  const handleZoomClose = () => {
    setZoomAnchorEl(null);
  };

  const handleZoomItemClick = newScale => () => {
    handleZoom(newScale);
    handleZoomClose();
  };

  const handlePipelineControlsClick = event => {
    setPipelineAnchorEl(event.currentTarget);
  };

  const handlePipelineDelete = async () => {
    await deletePipeline(currentPipeline);

    if (!error) history.push(`/${currentWorkspace.settings.name}`);
    handlePipelineControlsClose();
  };

  const handlePipelineControlsClose = () => {
    setPipelineAnchorEl(null);
  };

  const onToolboxClick = panel => {
    // If the Toolbox is not "open", we should open it before opening
    // the expansion panel
    if (!isToolboxOpen) handleToolboxOpen();
    handleToolbarClick(panel);
  };

  const getZoomDisplayedValue = value => {
    const percentage = value * 100;
    return `${Math.trunc(percentage)}%`;
  };

  return (
    <StyledToolbar>
      <div className="toolbox-controls">
        <ButtonGroup size="small">
          <Button onClick={() => onToolboxClick('source')}>
            <Tooltip title="Open Toolbox source panel" enterDelay={1000}>
              <FlightTakeoffIcon color="action" />
            </Tooltip>
          </Button>
          <Button onClick={() => onToolboxClick('topic')}>
            <Tooltip title="Open Toolbox topic panel" enterDelay={1000}>
              <StorageIcon color="action" />
            </Tooltip>
          </Button>
          <Button onClick={() => onToolboxClick('stream')}>
            <Tooltip title="Open Toolbox stream panel" enterDelay={1000}>
              <WavesIcon color="action" />
            </Tooltip>
          </Button>
          <Button onClick={() => onToolboxClick('sink')}>
            <Tooltip title="Open Toolbox sink panel" enterDelay={1000}>
              <FlightLandIcon color="action" />
            </Tooltip>
          </Button>
        </ButtonGroup>
        <Typography variant="body2">Insert</Typography>
      </div>
      <div className="paper-controls">
        <div className="zoom">
          <ButtonGroup size="small">
            <Button
              onClick={() => handleZoom(paperScale, 'out')}
              disabled={paperScale <= 0.02}
            >
              <Tooltip title="Zoom out" enterDelay={1000}>
                <RemoveIcon color="action" />
              </Tooltip>
            </Button>
            <Button
              onClick={handleZoomClick}
              variant="outlined"
              color="default"
              size="small"
            >
              {getZoomDisplayedValue(paperScale)}
            </Button>
            <Button
              onClick={() => handleZoom(paperScale, 'in')}
              disabled={paperScale >= 2}
            >
              <Tooltip title="Zoom in" enterDelay={1000}>
                <AddIcon color="action" />
              </Tooltip>
            </Button>
          </ButtonGroup>
          <Typography variant="body2">Zoom</Typography>

          <Menu
            anchorEl={zoomAnchorEl}
            keepMounted
            open={Boolean(zoomAnchorEl)}
            onClose={handleZoomClose}
          >
            <MenuItem onClick={handleZoomItemClick(0.5)}>50%</MenuItem>
            <MenuItem onClick={handleZoomItemClick(1)}>100%</MenuItem>
            <MenuItem onClick={handleZoomItemClick(2)}>200%</MenuItem>
          </Menu>
        </div>

        <div className="fit">
          <Button
            onClick={handleFit}
            variant="outlined"
            color="default"
            size="small"
          >
            <Tooltip
              title="Resize the paper to fit the content"
              enterDelay={1000}
            >
              <FullscreenIcon color="action" />
            </Tooltip>
          </Button>
          <Typography variant="body2">Fit</Typography>
        </div>

        <div className="center">
          <Button
            onClick={handleCenter}
            variant="outlined"
            color="default"
            size="small"
            disabled={!hasSelectedCell}
          >
            <Tooltip
              title="Move selected graph to the center"
              enterDelay={1000}
            >
              <FullscreenExitIcon color="action" />
            </Tooltip>
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
          <MenuItem
            onClick={() => {
              handlePipelineDelete();
            }}
          >
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
  handleToolbarClick: PropTypes.func.isRequired,
  isToolboxOpen: PropTypes.bool.isRequired,
  paperScale: PropTypes.number.isRequired,
  handleZoom: PropTypes.func.isRequired,
  handleFit: PropTypes.func.isRequired,
  handleCenter: PropTypes.func.isRequired,
  hasSelectedCell: PropTypes.bool.isRequired,
};

export default Toolbar;
