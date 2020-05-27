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
import styled from 'styled-components';
import Step from '@material-ui/core/Step';
import Dialog from '@material-ui/core/Dialog';
import Stepper from '@material-ui/core/Stepper';
import StepLabel from '@material-ui/core/StepLabel';
import DialogTitle from '@material-ui/core/DialogTitle';
import LinearProgress from '@material-ui/core/LinearProgress';

const StyledDiv = styled.div`
  margin-bottom: 15px;
`;

const Progress = props => {
  const {
    steps = [],
    open,
    activeStep,
    deleteType = false,
    createTitle = 'Creating',
    deleteTitle = 'Deleting',
    testId = 'step-testid',
    maxWidth = 'sm',
  } = props;

  const [completed, setCompleted] = useState(0);
  const [buffer, setBuffer] = useState(0);
  const [diff, setDiff] = useState(0);
  const [stepType, setStepType] = useState();
  const [color, setColor] = useState();
  const [title, setTitle] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const progress = useRef(() => {});

  useEffect(() => {
    progress.current = () => {
      if (deleteType) {
        if (title === createTitle) {
          setColor('secondary');
          setStepType('error');
          setTitle(deleteTitle);
          setBuffer(0);
          setDiff(0);
        } else {
          setCompleted((100 / steps.length) * activeStep);
        }
      } else {
        if (buffer > completed + diff) {
          setDiff(diff + Math.random() + 10);
        }
        if ((100 / steps.length) * activeStep + diff > 100) {
          setCompleted(100);
        } else {
          setCompleted((100 / steps.length) * activeStep + diff);
        }
        setBuffer((100 / steps.length) * (activeStep + 1));
      }
    };
  });

  useEffect(() => {
    const tick = () => {
      progress.current();
    };
    let timer;
    if (steps.length > activeStep && open) {
      setIsLoading(open);
      timer = setInterval(tick, 500);
    } else if (steps.length === activeStep && completed < 100) {
      timer = setInterval(tick, 500);
    } else if (completed === 100) {
      setIsLoading(false);
    }

    if (!open) {
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
  }, [activeStep, completed, createTitle, isLoading, open, steps.length]);

  return (
    <Dialog open={isLoading} maxWidth={maxWidth} fullWidth>
      <DialogTitle>{title}</DialogTitle>
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
      <LinearProgress
        color={color}
        valueBuffer={buffer}
        value={completed}
        variant="buffer"
      />
      <StyledDiv />
    </Dialog>
  );
};

Progress.propTypes = {
  steps: PropTypes.array.isRequired,
  open: PropTypes.bool.isRequired,
  activeStep: PropTypes.number.isRequired,
  deleteType: PropTypes.bool,
  createTitle: PropTypes.string,
  deleteTitle: PropTypes.string,
  maxWidth: PropTypes.string,
  testId: PropTypes.string,
};

export default Progress;
