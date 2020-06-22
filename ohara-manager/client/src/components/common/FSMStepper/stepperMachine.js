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

const config = {
  id: 'stepper',
  initial: 'auto',
  context: {
    activeStep: -1,
    steps: [],
    forward: true,
    error: null,
  },
  states: {
    idle: {
      on: {
        REVERT: {
          target: 'auto',
          actions: assign({ error: null, forward: (ctx) => !ctx.forward }),
        },
        RETRY: {
          target: '#stepper.auto.loading',
          actions: assign({ error: null }),
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
      on: {
        CLOSE: '',
        REVERT: {
          target: 'auto',
          actions: assign({ forward: (ctx) => !ctx.forward }),
        },
      },
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
  fireActionSuccess: () => {}, // TODO: May need to update some context
  fireActionFailure: assign((ctx, evt) => {
    ctx.error = evt;
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
