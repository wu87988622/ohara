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
import { map, merge, shuffle, take, times } from 'lodash';
import Stepper from '@material-ui/core/Stepper';
import Step from '@material-ui/core/Step';
import StepLabel from '@material-ui/core/StepLabel';
import StepContent from '@material-ui/core/StepContent';

import * as hooks from 'hooks';
import { CREATE_WORKSPACE_MODE, GROUP } from 'const';
import * as generate from 'utils/generate';
import { getKey } from 'utils/object';
import { hashByGroupAndName } from 'utils/sha';
import { Wrapper } from './CreateWorkspaceFormStyles';
import ReviewForm from './ReviewForm';
import SetupBrokerForm from './SetupBrokerForm';
import SetupNodesForm from './SetupNodesForm';
import SetupPluginsForm from './SetupPluginsForm';
import SetupWorkerForm from './SetupWorkerForm';
import SetupWorkspaceForm from './SetupWorkspaceForm';
import SetupZookeeperForm from './SetupZookeeperForm';

const { EXPERT, QUICK } = CREATE_WORKSPACE_MODE;

const CreateWorkspaceForm = props => {
  const { onCancel, onSubmit } = props;
  const mode = hooks.useCreateWorkspaceMode();
  const step = hooks.useCreateWorkspaceStep();
  const switchStep = hooks.useSwitchCreateWorkspaceStepAction();
  const nextStep = () => switchStep(step + 1);
  const previousStep = () => switchStep(step - 1);

  const randomTake = (array, n) => take(shuffle(array), n);

  const applyQuickRules = values => {
    const { name, nodeNames } = values.workspace;
    return merge(values, {
      zookeeper: {
        name,
        nodeNames: randomTake(nodeNames, nodeNames > 3 ? 3 : 1),
      },
      broker: { name, nodeNames },
      worker: { name, nodeNames },
    });
  };

  const applyGroup = values =>
    merge(values, {
      workspace: { group: GROUP.WORKSPACE },
      zookeeper: { group: GROUP.ZOOKEEPER },
      broker: { group: GROUP.BROKER },
      worker: { group: GROUP.WORKER },
      files: map(values.files, file => ({
        ...file,
        group: hashByGroupAndName(GROUP.WORKSPACE, values.workspace.name),
      })),
    });

  const handleSubmit = values => {
    if (mode === QUICK) {
      values = applyQuickRules(values);
    }

    const finalValues = merge(applyGroup(values), {
      broker: {
        zookeeperClusterKey: getKey(values.zookeeper),
      },
      worker: {
        brokerClusterKey: getKey(values.broker),
        freePorts: times(5, generate.port),
        plugins: map(values.files, file => getKey(file)),
      },
    });

    onSubmit(finalValues);
  };

  return (
    <Wrapper>
      <Stepper activeStep={step} orientation="vertical">
        <Step>
          <StepLabel>About this workspace</StepLabel>
          <StepContent>
            <SetupWorkspaceForm previousStep={onCancel} onSubmit={nextStep} />
          </StepContent>
        </Step>
        <Step>
          <StepLabel>Select nodes</StepLabel>
          <StepContent>
            <SetupNodesForm previousStep={previousStep} onSubmit={nextStep} />
          </StepContent>
        </Step>
        {mode === EXPERT && (
          <Step>
            <StepLabel>Setup zookeeper</StepLabel>
            <StepContent>
              <SetupZookeeperForm
                previousStep={previousStep}
                onSubmit={nextStep}
              />
            </StepContent>
          </Step>
        )}
        {mode === EXPERT && (
          <Step>
            <StepLabel>Setup broker</StepLabel>
            <StepContent>
              <SetupBrokerForm
                previousStep={previousStep}
                onSubmit={nextStep}
              />
            </StepContent>
          </Step>
        )}
        {mode === EXPERT && (
          <Step>
            <StepLabel>Setup worker</StepLabel>
            <StepContent>
              <SetupWorkerForm
                previousStep={previousStep}
                onSubmit={nextStep}
              />
            </StepContent>
          </Step>
        )}
        <Step>
          <StepLabel>Upload or select worker plugins(Optional)</StepLabel>
          <StepContent>
            <SetupPluginsForm previousStep={previousStep} onSubmit={nextStep} />
          </StepContent>
        </Step>
        <Step>
          <StepLabel>Create this workspace</StepLabel>
          <StepContent>
            <ReviewForm previousStep={previousStep} onSubmit={handleSubmit} />
          </StepContent>
        </Step>
      </Stepper>
      <div />
    </Wrapper>
  );
};

CreateWorkspaceForm.propTypes = {
  onCancel: PropTypes.func,
  onSubmit: PropTypes.func.isRequired,
};

CreateWorkspaceForm.defaultProps = {
  onCancel: () => {},
};

export default CreateWorkspaceForm;
