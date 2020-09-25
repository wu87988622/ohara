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
import { merge, shuffle, take, times } from 'lodash';
import { reduxForm } from 'redux-form';
import Stepper from '@material-ui/core/Stepper';
import Step from '@material-ui/core/Step';
import StepLabel from '@material-ui/core/StepLabel';
import StepContent from '@material-ui/core/StepContent';

import * as hooks from 'hooks';
import { CreateWorkspaceMode, Form, GROUP, KIND } from 'const';
import * as generate from 'utils/generate';
import { getKey } from 'utils/object';
import { Wrapper } from './CreateWorkspaceFormStyles';
import ReviewForm from './ReviewForm';
import SetupBrokerForm from './SetupBrokerForm';
import SetupNodesForm from './SetupNodesForm';
import SetupWorkerForm from './SetupWorkerForm';
import SetupWorkspaceForm from './SetupWorkspaceForm';
import SetupZookeeperForm from './SetupZookeeperForm';
import SetupVolumeForm from './SetupVolumeForm';

const { EXPERT, QUICK } = CreateWorkspaceMode;

const CreateWorkspaceForm = (props) => {
  const { onCancel, onSubmit } = props;
  const mode = hooks.useCreateWorkspaceMode();
  const step = hooks.useCreateWorkspaceStep();
  const volumesState = hooks.useAllVolumes();
  const switchStep = hooks.useSwitchCreateWorkspaceStepAction();
  const nextStep = () => switchStep(step + 1);
  const previousStep = () => switchStep(step - 1);

  const randomTake = (array, n) => take(shuffle(array), n);

  const volumes = (values, nodeNames, workspaceName) => {
    const volume = values?.volume;
    const zkVolumeLength =
      volumesState.filter((v) => v.tags.usedBy === KIND.zookeeper).length + 1;
    const bkVolumeLength =
      volumesState.filter((v) => v.tags.usedBy === KIND.broker).length + 1;
    if (volume) {
      const { path } = volume;
      return {
        zkVolume: {
          name: generate.serviceName(),
          nodeNames,
          path: `${path}/${KIND.zookeeper}`,
          tags: {
            usedBy: KIND.zookeeper,
            displayName: `zkv${zkVolumeLength}`,
            workspaceName,
          },
        },
        bkVolume: {
          name: generate.serviceName(),
          nodeNames,
          path: `${path}/${KIND.broker}`,
          tags: {
            usedBy: KIND.broker,
            displayName: `bkv${bkVolumeLength}`,
            workspaceName,
          },
        },
      };
    }
    return {};
  };

  const applyQuickRules = (values) => {
    const { name, nodeNames } = values.workspace;
    return merge(
      values,
      {
        zookeeper: {
          name,
          nodeNames: randomTake(nodeNames, nodeNames > 3 ? 3 : 1),
        },
        broker: { name, nodeNames },
        worker: { name, nodeNames },
      },
      volumes(values, nodeNames, name),
    );
  };

  const applyGroup = (values) =>
    merge(values, {
      workspace: { group: GROUP.WORKSPACE },
      zookeeper: { group: GROUP.ZOOKEEPER },
      broker: { group: GROUP.BROKER },
      worker: { group: GROUP.WORKER },
    });

  const handleSubmit = (values) => {
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
      },
    });

    if (finalValues.zkVolume && finalValues.bkVolume) {
      const zkVolume = finalValues.zkVolume;
      const bkVolume = finalValues.bkVolume;
      finalValues.zookeeper.dataDir = {
        name: zkVolume.name,
        group: GROUP.VOLUME,
      };
      finalValues.broker['log.dirs'] = [
        {
          name: bkVolume.name,
          group: GROUP.VOLUME,
        },
      ];
    }

    const {
      broker,
      worker,
      workspace,
      zookeeper,
      zkVolume,
      bkVolume,
    } = finalValues;
    onSubmit({ ...workspace, worker, broker, zookeeper, zkVolume, bkVolume });
  };

  return (
    <Wrapper>
      <Stepper activeStep={step} orientation="vertical">
        <Step>
          <StepLabel>About this workspace</StepLabel>
          <StepContent>
            <SetupWorkspaceForm onSubmit={nextStep} previousStep={onCancel} />
          </StepContent>
        </Step>
        <Step>
          <StepLabel>Select nodes</StepLabel>
          <StepContent>
            <SetupNodesForm onSubmit={nextStep} previousStep={previousStep} />
          </StepContent>
        </Step>
        <Step>
          <StepLabel>Set volumes(Optional)</StepLabel>
          <StepContent>
            <SetupVolumeForm onSubmit={nextStep} previousStep={previousStep} />
          </StepContent>
        </Step>
        {mode === EXPERT && (
          <Step>
            <StepLabel>Setup zookeeper</StepLabel>
            <StepContent>
              <SetupZookeeperForm
                onSubmit={nextStep}
                previousStep={previousStep}
              />
            </StepContent>
          </Step>
        )}
        {mode === EXPERT && (
          <Step>
            <StepLabel>Setup broker</StepLabel>
            <StepContent>
              <SetupBrokerForm
                onSubmit={nextStep}
                previousStep={previousStep}
              />
            </StepContent>
          </Step>
        )}
        {mode === EXPERT && (
          <Step>
            <StepLabel>Setup worker</StepLabel>
            <StepContent>
              <SetupWorkerForm
                onSubmit={nextStep}
                previousStep={previousStep}
              />
            </StepContent>
          </Step>
        )}
        <Step>
          <StepLabel>Create this workspace</StepLabel>
          <StepContent>
            <ReviewForm onSubmit={handleSubmit} previousStep={previousStep} />
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

export default reduxForm({
  form: Form.CREATE_WORKSPACE,
  destroyOnUnmount: false,
  forceUnregisterOnUnmount: true,
  initialValues: {
    workspace: {
      nodeNames: [],
    },
  },
})(CreateWorkspaceForm);
