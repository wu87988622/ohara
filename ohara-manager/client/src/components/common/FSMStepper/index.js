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

import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useMachine } from '@xstate/react';

import Button from '@material-ui/core/Button';
import Checkbox from '@material-ui/core/Checkbox';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Grid from '@material-ui/core/Grid';

import Controller from './Controller';
import LogViewer from './LogViewer';
import Progress from './Progress';
import Stepper from './Stepper';
import stepperMachine, {
  config as stepperMachineConfig,
} from './stepperMachine';
import { STEP_STAGES } from './const';
import Styles from './Styles';

const FSMStepper = React.forwardRef((props, ref) => {
  const {
    forceCloseAfterFinish,
    onClose,
    revertible,
    revertText,
    showController,
    showLogViewer,
    showProgress,
    showStepper,
    steps,
  } = props;

  const [state, send] = useMachine(
    stepperMachine.withContext({ ...stepperMachineConfig.context, steps }),
  );
  const isFinish = state.matches('finish');
  const hasError = !!state.context.error;
  const [closeChecked, setCloseChecked] = useState(forceCloseAfterFinish);

  useEffect(() => {
    if (closeChecked && isFinish && onClose) {
      setTimeout(onClose, 1000);
    }
  }, [closeChecked, onClose, isFinish]);

  // Apis
  React.useImperativeHandle(ref, () => ({
    getErrorLogs: () =>
      state?.context?.logs?.filter(
        (log) => log?.stepStage === STEP_STAGES.FAILURE,
      ),
    isFinish: () => isFinish,
  }));

  return (
    <Styles>
      <Grid container>
        {showStepper && (
          <Grid item xs={12}>
            <Stepper
              activeStep={state.context.activeStep}
              error={state.context.error}
              steps={state.context.steps}
            />
          </Grid>
        )}
        {showProgress && (
          <Grid item xs={12}>
            <Progress state={state} />
          </Grid>
        )}
        {showController && (
          <Grid item xs={12}>
            <Controller
              revertible={revertible}
              revertText={revertText}
              send={send}
              state={state}
            />
          </Grid>
        )}
        {showLogViewer && (
          <Grid item xs={12}>
            <LogViewer revertText={revertText} state={state} />
          </Grid>
        )}

        <Grid container justify="space-between">
          <FormControlLabel
            control={
              <Checkbox
                checked={closeChecked}
                color="primary"
                disabled={isFinish || forceCloseAfterFinish}
                onChange={(event) => setCloseChecked(event.target.checked)}
              />
            }
            label="Close after finish"
          />
          <Button
            data-testid="stepper-close-button"
            disabled={!(hasError || isFinish)}
            onClick={onClose}
          >
            CLOSE
          </Button>
        </Grid>
      </Grid>
    </Styles>
  );
});

FSMStepper.propTypes = {
  forceCloseAfterFinish: PropTypes.bool,
  onClose: PropTypes.func,
  revertible: PropTypes.bool,
  revertText: PropTypes.string,
  showController: PropTypes.bool,
  showLogViewer: PropTypes.bool,
  showProgress: PropTypes.bool,
  showStepper: PropTypes.bool,
  steps: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      action: PropTypes.func.isRequired,
      revertAction: PropTypes.func,
      delay: PropTypes.number, // Delay the execution of this step in milliseconds, default 0
    }),
  ).isRequired,
};

FSMStepper.defaultProps = {
  forceCloseAfterFinish: false,
  onClose: () => {},
  revertible: false,
  revertText: 'ROLLBACK',
  showController: true,
  showLogViewer: true,
  showProgress: true,
  showStepper: false,
};

export default FSMStepper;
