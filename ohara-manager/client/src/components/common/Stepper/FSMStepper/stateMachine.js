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

export const machineConfig = {
  id: 'Root',
  initial: 'auto',
  context: {
    forward: true,
    activeIndex: -1,
    activities: [],
  },
  states: {
    idle: {
      on: {
        RESUME: 'auto',
        REVERT: {
          target: 'auto',
          actions: assign({ forward: (ctx) => !ctx.forward }),
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
                actions: 'next',
                cond: 'hasNext',
              },
              { target: '#Root.finish', actions: 'next' },
            ],
          },
        },
        loading: {
          invoke: {
            id: 'fireAction',
            src: 'fireAction',
            onError: {
              target: '#Root.auto.failure',
              actions: 'fireActionFailure',
            },
            onDone: {
              target: '#Root.auto.success',
              actions: 'fireActionSuccess',
            },
          },
          on: {
            SUSPEND: 'cancelConfirming',
          },
        },
        success: {
          after: {
            0: 'unknown',
          },
        },
        failure: {
          on: {
            RETRY: 'loading',
          },
        },
        cancelConfirming: {
          on: {
            AGREE: 'cancelling',
            DISAGREE: '#Root.auto.loading',
          },
        },
        cancelling: {
          invoke: {
            id: 'cancelAction',
            src: 'cancelAction',
            onError: {
              target: '#Root.idle',
              actions: 'cancelActionFailure',
            },
            onDone: {
              target: '#Root.idle',
              actions: 'cancelActionSuccess',
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
  next: assign({
    activeIndex: (ctx) => {
      const { activeIndex, forward } = ctx;
      return forward ? activeIndex + 1 : activeIndex - 1;
    },
  }),
  fireActionSuccess: () => {}, // TODO: May need to update some context
  fireActionFailure: assign((ctx, evt) => {
    ctx.error = evt;
  }),
  cancelActionSuccess: () => {},
  cancelActionFailure: assign((ctx, evt) => {
    ctx.error = evt;
  }),
};

const guards = {
  hasNext: (ctx) => {
    const { activeIndex, activities, forward } = ctx;
    const nextActiveIndex = forward ? activeIndex + 1 : activeIndex - 1;
    const nextActivity = activities[nextActiveIndex];
    return !!nextActivity;
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
    const { activeIndex, activities, forward } = ctx;
    const activity = activities[activeIndex];
    const { action, delay = 0, revertAction } = activity;
    return forward
      ? delayFireAction(action, delay)
      : delayFireAction(revertAction, delay);
  },
  cancelAction: (ctx) => {
    const { activeIndex, activities, forward } = ctx;
    const activity = activities[activeIndex];
    if (forward) {
      return activity?.revertAction ? activity.revertAction() : null;
    } else {
      return activity?.action ? activity.action() : null;
    }
  },
};

export default Machine(machineConfig, {
  actions,
  guards,
  services,
});
