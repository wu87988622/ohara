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

import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { capitalize } from 'lodash';
import { useMachine } from '@xstate/react';
import Stepper from '@material-ui/core/Stepper';
import Step from '@material-ui/core/Step';
import StepLabel from '@material-ui/core/StepLabel';

import Controller from './Controller';
import LogViewer from './LogViewer';
import Progress from './Progress';
import machine, { machineConfig } from './stateMachine';
import { Styles } from './FSMStepperStyles';

const FSMStepper = (props) => {
  const { activities, onClose, rollback, showLog } = props;
  const [state, send] = useMachine(
    machine.withContext({ ...machineConfig.context, activities }),
  );

  const finish = state.matches('finish');
  useEffect(() => {
    if (finish && onClose) onClose();
  }, [finish, onClose]);

  return (
    <Styles>
      <Stepper
        activeStep={state.context.activeIndex}
        alternativeLabel
        className="stepper"
      >
        {state.context.activities
          .filter((activity) => !activity.hidden)
          .map((activity, index) => {
            const stepProps = {};
            const labelProps = {};
            if (state.context.error && index === state.context.activeIndex) {
              stepProps.completed = false;
              labelProps.error = true;
            }
            return (
              <Step key={activity?.name} {...stepProps}>
                <StepLabel {...labelProps}>
                  {activity?.name
                    ?.split(' ')
                    ?.map((segment) => capitalize(segment))
                    ?.join(' ')}
                </StepLabel>
              </Step>
            );
          })}
      </Stepper>
      <Progress state={state} />
      <Controller
        onClose={onClose}
        rollback={rollback}
        send={send}
        state={state}
      />
      {showLog && <LogViewer isOpen={true} />}
    </Styles>
  );
};

FSMStepper.propTypes = {
  activities: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      action: PropTypes.func.isRequired,
      revertAction: PropTypes.func,
      hidden: PropTypes.bool, // If true, this activity will not be displayed, default false
      delay: PropTypes.number, // Delay the execution of this activity in milliseconds, default 0
    }),
  ).isRequired,
  rollback: PropTypes.bool,
  showLog: PropTypes.bool,
  onClose: PropTypes.func,
};

FSMStepper.defaultProps = {
  rollback: false,
  showLog: false,
  onClose: () => {},
};

export default FSMStepper;
