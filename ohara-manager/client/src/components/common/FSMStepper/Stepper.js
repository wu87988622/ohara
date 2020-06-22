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
import { capitalize } from 'lodash';
import MuiStepper from '@material-ui/core/Stepper';
import MuiStep from '@material-ui/core/Step';
import MuiStepLabel from '@material-ui/core/StepLabel';

const Stepper = (props) => {
  const { activeStep, error, steps } = props;

  return (
    <MuiStepper activeStep={activeStep} alternativeLabel className="stepper">
      {steps
        .filter((step) => !step.hidden)
        .map((step, index) => {
          const stepProps = {};
          const labelProps = {};
          if (error && index === activeStep) {
            stepProps.completed = false;
            labelProps.error = true;
          }
          return (
            <MuiStep key={step?.name} {...stepProps}>
              <MuiStepLabel {...labelProps}>
                {step?.name
                  ?.split(' ')
                  ?.map((segment) => capitalize(segment))
                  ?.join(' ')}
              </MuiStepLabel>
            </MuiStep>
          );
        })}
    </MuiStepper>
  );
};

Stepper.propTypes = {
  activeStep: PropTypes.number.isRequired,
  error: PropTypes.object,
  steps: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      hidden: PropTypes.bool,
    }),
  ).isRequired,
};

export default Stepper;
