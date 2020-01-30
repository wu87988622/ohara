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
import { useApi } from 'context';
import { useEventLog } from 'utils/hooks';
import { createActions } from './brokerActions';
import { reducer, initialState } from './brokerReducer';

const BrokerStateContext = React.createContext();
const BrokerDispatchContext = React.createContext();

const BrokerProvider = ({ children }) => {
  const [state, dispatch] = React.useReducer(reducer, initialState);
  const eventLog = useEventLog();
  const { brokerApi } = useApi();

  React.useEffect(() => {
    if (!brokerApi) return;
    const actions = createActions({ state, dispatch, eventLog, brokerApi });
    actions.fetchBrokers();
  }, [state, dispatch, eventLog, brokerApi]);

  return (
    <BrokerStateContext.Provider value={state}>
      <BrokerDispatchContext.Provider value={dispatch}>
        {children}
      </BrokerDispatchContext.Provider>
    </BrokerStateContext.Provider>
  );
};

BrokerProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

const useBrokerState = () => {
  const context = React.useContext(BrokerStateContext);
  if (context === undefined) {
    throw new Error('useBrokerState must be used within a BrokerProvider');
  }
  return context;
};

const useBrokerDispatch = () => {
  const context = React.useContext(BrokerDispatchContext);
  if (context === undefined) {
    throw new Error('useBrokerDispatch must be used within a BrokerProvider');
  }
  return context;
};

const useBrokerActions = () => {
  const state = useBrokerState();
  const dispatch = useBrokerDispatch();
  const eventLog = useEventLog();
  const { brokerApi } = useApi();
  return React.useMemo(
    () => createActions({ state, dispatch, eventLog, brokerApi }),
    [state, dispatch, eventLog, brokerApi],
  );
};

export { BrokerProvider, useBrokerState, useBrokerDispatch, useBrokerActions };
