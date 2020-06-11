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

import React, { useEffect, useState, useRef } from 'react';
import PropTypes from 'prop-types';
import Step from '@material-ui/core/Step';
import Stepper from '@material-ui/core/Stepper';
import Checkbox from '@material-ui/core/Checkbox';
import StepLabel from '@material-ui/core/StepLabel';
import DialogTitle from '@material-ui/core/DialogTitle';
import ArrowDropUpIcon from '@material-ui/icons/ArrowDropUp';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Card from '@material-ui/core/Card';
import Button from '@material-ui/core/Button';
import IconButton from '@material-ui/core/IconButton';
import FormControl from '@material-ui/core/FormControl';
import LinearProgress from '@material-ui/core/LinearProgress';
import Typography from '@material-ui/core/Typography';

import { VirtualizedList } from 'components/common/List';
import LogRow from './LogRow';
import * as s from './LogPropessStyles';
import { DeleteDialog } from 'components/common/Dialog';
import * as hooks from 'hooks';
import { useEventLogContentDialog } from 'context';
import EventLogContentDialog from 'components/EventLog/EventLogContentDialog';

const LogProgress = (props) => {
  const {
    steps = [],
    isOpen,
    activeStep,
    createTitle = 'Creating',
    testId = 'step-testid',
    message = null,
    onPause,
    isPause,
    onResume,
    onRollback,
    onClose,
    onAutoClose,
    isAutoClose,
    closeDisable,
  } = props;

  const [completed, setCompleted] = useState(0);
  const [buffer, setBuffer] = useState(0);
  const [diff, setDiff] = useState(0);
  const [stepType, setStepType] = useState();
  const [color, setColor] = useState();
  const [title, setTitle] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isHidden, setIsHidden] = useState(true);
  const [
    isRollbackConfirmDialogOpen,
    setIsRollbackConfirmDialogOpen,
  ] = useState(false);
  const log = hooks.useLogProgress().data;
  const { open: openEventLogContentDialog } = useEventLogContentDialog();
  const handleRowClick = (rowData) => openEventLogContentDialog(rowData);

  const progress = useRef(() => {});

  const handelHideButton = () => {
    if (isHidden) {
      setIsHidden(false);
    } else {
      setIsHidden(true);
    }
  };

  useEffect(() => {
    progress.current = () => {
      if (buffer > completed + diff) {
        setDiff(diff + 5);
      }
      if ((100 / steps.length) * activeStep + diff > 100) {
        setCompleted(100);
      } else {
        setCompleted((100 / steps.length) * activeStep + diff);
      }
      setBuffer((100 / steps.length) * (activeStep + 1));
    };
  });

  useEffect(() => {
    const tick = () => {
      progress.current();
    };
    let timer;
    if (steps.length > activeStep && isOpen) {
      setIsLoading(isOpen);
      timer = setInterval(tick, 500);
    } else if (steps.length === activeStep && completed < 100) {
      timer = setInterval(tick, 500);
    } else if (completed === 100 && isAutoClose) {
      setIsLoading(false);
    }

    if (!isOpen) {
      setIsLoading(false);
    }

    if (!isLoading) {
      setCompleted(0);
      setColor();
      setStepType();
      setTitle(createTitle);
      setBuffer(0);
      setDiff(0);
      clearInterval(timer);
    }
    return () => {
      clearInterval(timer);
    };
  }, [
    steps.length,
    activeStep,
    isOpen,
    completed,
    isLoading,
    createTitle,
    isAutoClose,
  ]);

  return (
    <s.StyledDialog
      fullWidth
      isDeleteDialog={createTitle === 'Delete Workspace'}
      isHidden={isHidden}
      maxWidth={'md'}
      open={isLoading}
    >
      <DialogTitle>{title}</DialogTitle>
      {!isHidden && (
        <Stepper activeStep={activeStep}>
          {steps.map((step) => {
            let type = {};
            if (stepType === 'error') {
              type.error = true;
            }
            return (
              <Step data-testid={testId} key={step}>
                <StepLabel {...type}>{step}</StepLabel>
              </Step>
            );
          })}
        </Stepper>
      )}
      <div className={'FlexDiv'}>
        <LinearProgress
          className={'StyledProgress'}
          color={color}
          value={completed}
          valueBuffer={buffer}
          variant="determinate"
        />
        {false && (
          <div className={'RightFlexDiv'}>
            {isPause ? (
              <Button
                className={'SuspendButton'}
                color="primary"
                onClick={() => setIsRollbackConfirmDialogOpen(true)}
              >
                ROLLBACK
              </Button>
            ) : (
              <Button
                className={'SuspendButton'}
                color="primary"
                onClick={onPause}
              >
                SUSPEND
              </Button>
            )}
            {isPause && (
              <Button
                className={'SuspendButton'}
                color="primary"
                onClick={onResume}
              >
                RESUME
              </Button>
            )}
          </div>
        )}
      </div>
      <div className={'FlexIconButtonDiv'}>
        {isHidden && (
          <Typography className={'StyledTypography'}>{message}</Typography>
        )}
        <IconButton
          className={'StyledIconButton'}
          onClick={handelHideButton}
          size="small"
        >
          {!isHidden ? <ArrowDropUpIcon /> : <ArrowDropDownIcon />}
        </IconButton>
      </div>
      {!isHidden && (
        <Card className={'StyledCard'}>
          <>
            <VirtualizedList
              autoScrollToBottom
              data={log}
              onRowClick={handleRowClick}
              rowRenderer={LogRow}
            />
            <EventLogContentDialog />
          </>
        </Card>
      )}
      <div className={'FlexFooterDiv'}>
        <FormControl className={'StyledFormControl'}>
          <FormControlLabel
            control={<Checkbox onClick={onAutoClose} />}
            label="Close after successful restart"
          />
        </FormControl>
        <Button
          className={'StyledCloseButton'}
          disabled={closeDisable}
          onClick={onClose}
        >
          CLOSE
        </Button>
      </div>
      <DeleteDialog
        confirmText="Rollback"
        content="We will use the original settings before restarting to restore your workspace."
        onClose={() => setIsRollbackConfirmDialogOpen(false)}
        onConfirm={() => {
          setIsRollbackConfirmDialogOpen(false);
          onRollback();
        }}
        open={isRollbackConfirmDialogOpen}
        title="Are you absolutely sure?"
      />
    </s.StyledDialog>
  );
};

LogProgress.propTypes = {
  steps: PropTypes.arrayOf(PropTypes.string).isRequired,
  isOpen: PropTypes.bool.isRequired,
  activeStep: PropTypes.number.isRequired,
  createTitle: PropTypes.string,
  deleteTitle: PropTypes.string,
  testId: PropTypes.string,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
  message: PropTypes.string,
  onPause: PropTypes.func,
  onResume: PropTypes.func,
  onClick: PropTypes.func,
  isPause: PropTypes.bool,
  onRollback: PropTypes.func,
  onAutoClose: PropTypes.func,
  onClose: PropTypes.func,
  isAutoClose: PropTypes.bool,
  closeDisable: PropTypes.bool,
};

export default LogProgress;
