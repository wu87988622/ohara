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
import { useHistory } from 'react-router-dom';
import _ from 'lodash';

import { KIND } from 'const';
import { Progress } from 'components/common/Progress';
import { useEventLog } from 'context/eventLog/eventLogHooks';
import { StyledToolbar } from './ToolbarStyles';
import { Button } from 'components/common/Form';
import { useDeleteServices, useZoom } from './ToolbarHooks';
import { Tooltip } from 'components/common/Tooltip';
import * as pipelineContext from '../Pipeline';
import * as context from 'context';

const Toolbar = props => {
  const { handleToolboxOpen, handleToolbarClick, isToolboxOpen } = props;

  const [pipelineAnchorEl, setPipelineAnchorEl] = React.useState(null);
  const [zoomAnchorEl, setZoomAnchorEl] = React.useState(null);
  const [isDeletingPipeline, setIsDeletingPipeline] = React.useState(false);
  const pipelineDispatch = React.useContext(
    pipelineContext.PipelineDispatchContext,
  );
  const { isMetricsOn } = React.useContext(
    pipelineContext.PipelineStateContext,
  );

  const { deletePipeline } = context.usePipelineActions();
  const { startConnector, stopConnector } = context.useConnectorActions();
  const { startStream, stopStream } = context.useStreamActions();
  const { currentWorkspace, currentPipeline, error } = context.useWorkspace();
  const { selectedCell } = context.usePipelineState();

  const { steps, activeStep, deleteServices } = useDeleteServices();
  const { setZoom, scale, setScale } = useZoom();
  const history = useHistory();
  const paperApi = React.useContext(pipelineContext.PaperContext);
  const eventLog = useEventLog();

  const handleZoomClick = event => {
    setZoomAnchorEl(event.currentTarget);
  };

  const handleZoomClose = () => {
    setZoomAnchorEl(null);
  };

  const handleZoom = (scale, instruction) => {
    setZoom(scale, instruction);
    if (instruction === 'fromDropdown') handleZoomClose();
  };

  const handlePipelineControlsClick = event => {
    setPipelineAnchorEl(event.currentTarget);
  };

  const makeRequest = (pipeline, action) => {
    const { objects: services } = pipeline;

    const connectors = services.filter(
      service => service.kind === KIND.source || service.kind === KIND.sink,
    );
    const streams = services.filter(service => service.kind === KIND.stream);

    let connectorPromises = [];
    let streamsPromises = [];

    if (action === 'start') {
      connectorPromises = connectors.map(({ name }) => startConnector(name));
      streamsPromises = streams.map(({ name }) => startStream(name));
    } else {
      connectorPromises = connectors.map(({ name }) => stopConnector(name));
      streamsPromises = streams.map(({ name }) => stopStream(name));
    }
    return Promise.all([...connectorPromises, ...streamsPromises]).then(
      result => result,
    );
  };

  const handlePipelineStart = async () => {
    await makeRequest(currentPipeline, 'start');
    handlePipelineControlsClose();
  };

  const handlePipelineStop = async () => {
    await makeRequest(currentPipeline, 'stop');
    handlePipelineControlsClose();
  };

  const handlePipelineDelete = async () => {
    setIsDeletingPipeline(true);
    const { objects: services, name } = currentPipeline;

    await deleteServices(services);
    const res = await deletePipeline(name);
    if (!res.error) {
      eventLog.info(`Successfully deleted pipeline ${name}.`);
    }
    setIsDeletingPipeline(false);

    if (!error) history.push(`/${currentWorkspace.name}`);
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

  const getZoomDisplayedValue = scale => {
    const percentage = scale * 100;
    return `${Math.trunc(percentage)}%`;
  };

  return (
    <StyledToolbar>
      <div className="toolbox-controls">
        <ButtonGroup size="small">
          <Button onClick={() => onToolboxClick('source')}>
            <Tooltip title="Open Toolbox source panel">
              <FlightTakeoffIcon color="action" />
            </Tooltip>
          </Button>
          <Button onClick={() => onToolboxClick('topic')}>
            <Tooltip title="Open Toolbox topic panel">
              <StorageIcon color="action" />
            </Tooltip>
          </Button>
          <Button onClick={() => onToolboxClick('stream')}>
            <Tooltip title="Open Toolbox stream panel">
              <WavesIcon color="action" />
            </Tooltip>
          </Button>
          <Button onClick={() => onToolboxClick('sink')}>
            <Tooltip title="Open Toolbox sink panel">
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
              onClick={() => handleZoom(scale, 'out')}
              disabled={scale <= 0.02}
            >
              <Tooltip title="Zoom out">
                <RemoveIcon color="action" />
              </Tooltip>
            </Button>
            <Button
              onClick={handleZoomClick}
              variant="outlined"
              color="default"
              size="small"
            >
              {getZoomDisplayedValue(scale)}
            </Button>
            <Button
              onClick={() => handleZoom(scale, 'in')}
              disabled={scale >= 2}
            >
              <Tooltip title="Zoom in">
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
            <MenuItem onClick={() => handleZoom(0.5, 'fromDropdown')}>
              50%
            </MenuItem>
            <MenuItem onClick={() => handleZoom(1, 'fromDropdown')}>
              100%
            </MenuItem>
            <MenuItem onClick={() => handleZoom(2, 'fromDropdown')}>
              200%
            </MenuItem>
          </Menu>
        </div>

        <div className="fit">
          <Tooltip title="Resize the paper to fit the content">
            <Button
              onClick={() => {
                const newScale = paperApi.fit();
                setScale(newScale);
              }}
              variant="outlined"
              color="default"
              size="small"
            >
              <FullscreenIcon color="action" />
            </Button>
          </Tooltip>
          <Typography variant="body2">Fit</Typography>
        </div>

        <div className="center">
          <Tooltip title="Move selected graph to the center">
            <Button
              onClick={() => paperApi.center(selectedCell.id)}
              variant="outlined"
              color="default"
              size="small"
              disabled={!selectedCell}
            >
              <FullscreenExitIcon color="action" />
            </Button>
          </Tooltip>
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
          <MenuItem onClick={handlePipelineStart}>
            Start all components
          </MenuItem>
          <MenuItem onClick={handlePipelineStop}>Stop all components</MenuItem>
          <MenuItem onClick={handlePipelineDelete}>
            Delete this pipeline
          </MenuItem>
        </Menu>
      </div>

      {isMetricsOn !== null && (
        <div className="metrics-controls">
          <FormControlLabel
            control={
              <Switch
                checked={isMetricsOn}
                onChange={() => {
                  pipelineDispatch({ type: 'toggleMetricsButton' });
                  paperApi.toggleMetrics(!isMetricsOn);
                }}
                color="primary"
              />
            }
          />
          <Typography variant="body2">Metrics</Typography>
        </div>
      )}

      {// Display the progress when deleting if there are running objects
      !_.isEmpty(steps) && (
        <Progress
          open={isDeletingPipeline}
          createTitle={`Deleting pipeline ${currentPipeline.name}`}
          steps={steps}
          activeStep={activeStep}
        />
      )}
    </StyledToolbar>
  );
};

Toolbar.propTypes = {
  handleToolboxOpen: PropTypes.func.isRequired,
  handleToolbarClick: PropTypes.func.isRequired,
  isToolboxOpen: PropTypes.bool.isRequired,
};

export default Toolbar;
