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

import { Machine, assign } from 'xstate';

import { LOG_TYPES, STEP_STAGES, EVENTS } from './const';
import { createLog } from './utils';

const config = {
  id: 'stepper',
  initial: 'auto',
  context: {
    activeStep: -1,
    steps: [],
    forward: true,
    error: null,
    logs: [],
  },
  states: {
    idle: {
      on: {
        REVERT: {
          target: 'auto',
          actions: [
            'log',
            assign({
              error: null,
              forward: (ctx) => !ctx.forward,
              logs: (ctx) => [
                ...ctx.logs,
                createLog({ title: EVENTS.REVERT, type: LOG_TYPES.EVENT }),
              ],
            }),
          ],
        },
        RETRY: {
          target: '#stepper.auto.loading',
          actions: [
            'log',
            assign({
              error: null,
              logs: (ctx) => [
                ...ctx.logs,
                createLog({ title: EVENTS.RETRY, type: LOG_TYPES.EVENT }),
              ],
            }),
          ],
        },
      },
    },
    auto: {
      initial: 'unknown',
      states: {
        unknown: {
          on: {
            '': [
              {
                target: 'loading',
                actions: 'toNextStep',
                cond: 'hasNextStep',
              },
              { target: '#stepper.finish', actions: 'toNextStep' },
            ],
          },
        },
        loading: {
          invoke: {
            id: 'fireAction',
            src: 'fireAction',
            onError: {
              target: '#stepper.idle',
              actions: 'fireActionFailure',
            },
            onDone: {
              target: '#stepper.auto',
              actions: 'fireActionSuccess',
            },
          },
        },
      },
    },
    finish: {
      type: 'final',
    },
  },
};

const actions = {
  toNextStep: assign({
    activeStep: (ctx) => {
      const { activeStep, forward } = ctx;
      return forward ? activeStep + 1 : activeStep - 1;
    },
  }),
  fireActionSuccess: assign({
    logs: (ctx) => {
      const step = ctx.steps[ctx.activeStep];
      return [
        ...ctx.logs,
        createLog({
          title: step.name,
          type: LOG_TYPES.STEP,
          stepStage: STEP_STAGES.SUCCESS,
          isRevert: !ctx.forward,
        }),
      ];
    },
  }),
  fireActionFailure: assign({
    error: (ctx, evt) => {
      return evt;
    },
    logs: (ctx, evt) => {
      const step = ctx.steps[ctx.activeStep];
      return [
        ...ctx.logs,
        createLog({
          title: step.name,
          type: LOG_TYPES.STEP,
          stepStage: STEP_STAGES.FAILURE,
          payload: evt.data,
          isRevert: !ctx.forward,
        }),
      ];
    },
  }),
};

const guards = {
  hasNextStep: (ctx) => {
    const { activeStep, steps, forward } = ctx;
    const nextStep = forward ? steps[activeStep + 1] : steps[activeStep - 1];
    return !!nextStep;
  },
};

const delayFireAction = (action, delay = 0) => {
  if (!action) return null;
  return new Promise((resolve, reject) => {
    setTimeout(() => action().then(resolve).catch(reject), delay);
  });
};

const services = {
  fireAction: (ctx) => {
    const { activeStep, steps, forward } = ctx;
    const step = steps[activeStep];
    const { action, delay = 0, revertAction } = step;
    return forward
      ? delayFireAction(action, delay)
      : delayFireAction(revertAction, delay);
  },
};

const stepperMachine = Machine(config, {
  actions,
  guards,
  services,
});

export default stepperMachine;
export { config };
