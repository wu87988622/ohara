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

const LogProgress = props => {
  const {
    steps = [],
    isOpen,
    activeStep,
    createTitle = 'Creating',
    testId = 'step-testid',
    data = [],
    message = null,
    onPause,
    isPause,
    onResume,
    onRollback,
  } = props;

  const [completed, setCompleted] = useState(0);
  const [buffer, setBuffer] = useState(0);
  const [diff, setDiff] = useState(0);
  const [oldActiveStep, setOldActiveStep] = useState(0);
  const [stepType, setStepType] = useState();
  const [color, setColor] = useState();
  const [title, setTitle] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isHide, setIsHide] = useState(true);

  const progress = useRef(() => {});

  const handelHideButton = () => {
    if (isHide) {
      setIsHide(false);
    } else {
      setIsHide(true);
    }
  };

  useEffect(() => {
    progress.current = () => {
      if (steps.length === activeStep) {
        setCompleted(100);
      }
      if (buffer > completed + diff) {
        setDiff(diff + Math.random() + 10);
      }
      if (activeStep > oldActiveStep) {
        setCompleted((100 / steps.length) * activeStep);
        setDiff(0);
      } else {
        setCompleted((100 / steps.length) * activeStep + diff);
      }
      setBuffer((100 / steps.length) * (activeStep + 1));
      setOldActiveStep(activeStep);
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
    } else if (completed === 100) {
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
  }, [steps.length, activeStep, isOpen, completed, isLoading, createTitle]);

  return (
    <s.StyledDialog open={isLoading} maxWidth={'md'} fullWidth isHide={isHide}>
      <DialogTitle>{title}</DialogTitle>
      {!isHide && (
        <Stepper activeStep={activeStep}>
          {steps.map(step => {
            let type = {};
            if (stepType === 'error') {
              type.error = true;
            }
            return (
              <Step key={step} data-testid={testId}>
                <StepLabel {...type}>{step}</StepLabel>
              </Step>
            );
          })}
        </Stepper>
      )}
      <div className={'FlexDiv'} isHide={isHide}>
        <LinearProgress
          className={'StyledProgress'}
          color={color}
          valueBuffer={buffer}
          value={completed}
          variant="determinate"
          isHide={isHide}
        />

        <div className={'RightFlexDiv'} isHide={isHide}>
          {isPause ? (
            <Button
              className={'SuspendButton'}
              color="primary"
              onClick={onRollback}
              isHide={isHide}
            >
              ROLLBACK
            </Button>
          ) : (
            <Button
              className={'SuspendButton'}
              color="primary"
              onClick={onPause}
              isHide={isHide}
            >
              SUSPEND
            </Button>
          )}
          {isPause && (
            <Button
              className={'SuspendButton'}
              color="primary"
              onClick={onResume}
              isHide={isHide}
            >
              RESUME
            </Button>
          )}
        </div>
      </div>
      <div className={'FlexIconButtonDiv'}>
        {isHide && (
          <Typography className={'StyledTypography'}>{message}</Typography>
        )}
        <IconButton
          className={'StyledIconButton'}
          onClick={handelHideButton}
          size="small"
        >
          {!isHide ? <ArrowDropUpIcon /> : <ArrowDropDownIcon />}
        </IconButton>
      </div>
      {!isHide && (
        <Card className={'StyledCard'}>
          <VirtualizedList
            autoScrollToBottom
            data={data}
            rowRenderer={LogRow}
          />
        </Card>
      )}
      <div className={'FlexFooterDiv'}>
        <FormControl className={'StyledFormControl'}>
          <FormControlLabel
            control={<Checkbox />}
            label="Close after successful restart"
          />
        </FormControl>
        <Button className={'StyledCloseButton'}>CLOSE</Button>
      </div>
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
  isPause: PropTypes.bool,
  onRollback: PropTypes.func,
};

export default LogProgress;
