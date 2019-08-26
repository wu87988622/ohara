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

const Progress = props => {
  const {
    steps = [],
    open,
    activeStep,
    deleteType = false,
    createTitle = 'Creating',
    deleteTitle = 'Deleting',
    testId = 'step-testid',
  } = props;
  const [completed, setCompleted] = useState(0);
  const [buffer, setBuffer] = useState(0);
  const [diff, setDiff] = useState(0);
  const [oldActiveStep, setOldActiveStep] = useState(0);
  const [stepType, setStepType] = useState();
  const [color, setColor] = useState();
  const [title, setTitle] = useState('');

  const StyledDiv = styled.div`
    margin-bottom: 15px;
  `;

  const progress = useRef(() => {});
  useEffect(() => {
    progress.current = () => {
      if (completed < 100) {
        if (deleteType) {
          if (title === createTitle) {
            setColor('secondary');
            setStepType('error');
            setTitle(deleteTitle);
            setBuffer(0);
            setDiff(0);
          } else {
            setDiff(diff - 2);
            setCompleted((100 / steps.length) * activeStep + diff);
          }
        } else {
          setDiff(diff + Math.random() + 10);
          if (activeStep > oldActiveStep) {
            setCompleted((100 / steps.length) * activeStep);
            setDiff(0);
          } else {
            setCompleted((100 / steps.length) * activeStep + diff);
          }
          setBuffer((100 / steps.length) * (activeStep + 1));
          setOldActiveStep(activeStep);
        }
      }
    };
  });

  useEffect(() => {
    const tick = () => {
      progress.current();
    };
    let timer;
    if (steps.length > activeStep && open) {
      timer = setInterval(tick, 500);
    }
    if (!open) {
      setCompleted(0);
      setColor();
      setStepType();
      setTitle(createTitle);
      setBuffer(0);
      setDiff(0);
    }
    return () => {
      clearInterval(timer);
    };
  }, [activeStep, createTitle, open, steps.length]);

  return (
    <Dialog open={open} fullWidth>
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
  testId: PropTypes.string,
};

export default Progress;
