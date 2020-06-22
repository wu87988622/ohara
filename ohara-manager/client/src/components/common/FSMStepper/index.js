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
import { useMachine } from '@xstate/react';

import Controller from './Controller';
import LogViewer from './LogViewer';
import Progress from './Progress';
import Stepper from './Stepper';
import stepperMachine, {
  config as stepperMachineConfig,
} from './stepperMachine';
import Styles from './Styles';

const FSMStepper = (props) => {
  const { steps, onClose, revertible, showLog } = props;
  const [state, send] = useMachine(
    stepperMachine.withContext({ ...stepperMachineConfig.context, steps }),
  );

  const finish = state.matches('finish');
  useEffect(() => {
    if (finish && onClose) onClose();
  }, [finish, onClose]);

  return (
    <Styles>
      <Stepper
        activeStep={state.context.activeStep}
        error={state.context.error}
        steps={state.context.steps}
      />
      <Progress
        activeStep={state.context.activeStep}
        error={state.context.error}
        steps={state.context.steps}
      />
      <Controller
        onClose={onClose}
        revertible={revertible}
        send={send}
        state={state}
      />
      {showLog && <LogViewer isOpen={true} />}
    </Styles>
  );
};

FSMStepper.propTypes = {
  steps: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      action: PropTypes.func.isRequired,
      revertAction: PropTypes.func,
      hidden: PropTypes.bool, // If true, this step will not be displayed, default false
      delay: PropTypes.number, // Delay the execution of this step in milliseconds, default 0
    }),
  ).isRequired,
  revertible: PropTypes.bool,
  showLog: PropTypes.bool,
  onClose: PropTypes.func,
};

FSMStepper.defaultProps = {
  revertible: false,
  showLog: false,
  onClose: () => {},
};

export default FSMStepper;
