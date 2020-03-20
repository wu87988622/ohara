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
import IconButton from '@material-ui/core/IconButton';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import MoreIcon from '@material-ui/icons/MoreVert';
import Typography from '@material-ui/core/Typography';

import * as context from 'context';
import * as hooks from 'hooks';
import { KIND } from 'const';
import { Dialog } from 'components/common/Dialog';
import { Progress } from 'components/common/Progress';

function MoreActions() {
  const [anchorEl, setAnchorEl] = React.useState(null);
  const [isOpen, setIsOpne] = React.useState(false);
  const [isResetting, setIsResetting] = React.useState(false);
  const [activeStep, setActiveStep] = React.useState(0);

  const currentZookeeper = hooks.useZookeeper();
  const currentBroker = hooks.useBroker();
  const currentWorker = hooks.useWorker();
  const currentPipeline = hooks.usePipeline();

  const { stopConnector, startConnector } = context.useConnectorActions();
  const { stopTopic, startTopic } = context.useTopicActions();
  const { stopStream, startStream } = context.useStreamActions();

  const startBroker = hooks.useStartBrokerAction();
  const startWorker = hooks.useStartWorkerAction();
  const startZookeeper = hooks.useStartZookeeperAction();
  const stopBroker = hooks.useStopBrokerAction();
  const stopWorker = hooks.useStopWorkerAction();
  const stopZookeeper = hooks.useStopZookeeperAction();

  const handleClick = event => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleDialogClose = () => {
    setIsOpne(false);
  };

  const steps = [
    'Stop Pipeline',
    'Stop Workspace',
    'Start Workspace',
    'Start Pipeline',
  ];

  const reset = async () => {
    setIsResetting(true);
    const objects = currentPipeline.objects;
    const connectors = objects
      .filter(
        object => object.kind === KIND.source || object.kind === KIND.sink,
      )
      .filter(object => object.state === 'RUNNING');
    const topics = objects
      .filter(object => object.kind === KIND.topic)
      .filter(object => object.state === 'RUNNING');
    const streams = objects
      .filter(object => object.kind === KIND.stream)
      .filter(object => object.state === 'RUNNING');

    for (const connector of connectors) {
      await stopConnector(connector.name);
    }
    for (const stream of streams) {
      await stopStream(stream.name);
    }
    for (const topic of topics) {
      await stopTopic(topic.name);
    }

    setActiveStep(1);
    await stopWorker(currentWorker.name);
    await stopBroker(currentBroker.name);
    await stopZookeeper(currentZookeeper.name);
    setActiveStep(2);
    await startZookeeper(currentZookeeper.name);
    await startBroker(currentBroker.name);
    await startWorker(currentWorker.name);
    setActiveStep(3);

    for (const topic of topics) {
      await startTopic(topic.name);
    }
    for (const connector of connectors) {
      await startConnector(connector.name);
    }
    for (const stream of streams) {
      await startStream(stream.name);
    }

    setActiveStep(4);
    setIsOpne(false);
    handleClose();
  };
  return (
    <>
      <IconButton
        aria-label="display more actions"
        edge="end"
        color="inherit"
        onClick={handleClick}
      >
        <MoreIcon />
      </IconButton>
      <Menu
        id="edit-workspace-more-actions-menu"
        anchorEl={anchorEl}
        keepMounted
        open={Boolean(anchorEl)}
        onClose={handleClose}
      >
        <MenuItem onClick={() => setIsOpne(true)}>RESET WORKSPACE</MenuItem>
        {/* Feature is disabled because it's not implemented in 0.9 */
        false && <MenuItem onClick={handleClose}>ADD PLUGINS</MenuItem>}
        {/* Feature is disabled because it's not implemented in 0.9 */
        false && <MenuItem onClick={handleClose}>ADD NODES</MenuItem>}
      </Menu>
      <Dialog
        open={isOpen}
        title={'Reset Workspace?'}
        children={
          <Typography>
            Please be reminded that this reset will restart all services that
            are currently running. There are changes that the data being
            processed could be lost and there’s no way to undo this action.
          </Typography>
        }
        confirmText={'RESET'}
        handleClose={handleDialogClose}
        handleConfirm={reset}
      />
      <Progress
        createTitle={'Resetting'}
        open={isResetting}
        steps={steps}
        activeStep={activeStep}
        maxWidth="md"
      />
    </>
  );
}

export default MoreActions;
