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
import { useSnackbar } from 'context/SnackbarContext';
import {
  fetchBrokersCreator,
  addBrokerCreator,
  updateBrokerCreator,
  deleteBrokerCreator,
} from './brokerActions';
import { reducer, initialState } from './brokerReducer';

const BrokerStateContext = React.createContext();
const BrokerDispatchContext = React.createContext();

const BrokerProvider = ({ children }) => {
  const [state, dispatch] = React.useReducer(reducer, initialState);
  const showMessage = useSnackbar();

  const fetchBrokers = React.useCallback(
    fetchBrokersCreator(state, dispatch, showMessage),
    [state],
  );

  React.useEffect(() => {
    fetchBrokers();
  }, [fetchBrokers]);

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
  const showMessage = useSnackbar();
  return {
    addBroker: addBrokerCreator(state, dispatch, showMessage),
    updateBroker: updateBrokerCreator(state, dispatch, showMessage),
    deleteBroker: deleteBrokerCreator(state, dispatch, showMessage),
  };
};

export { BrokerProvider, useBrokerState, useBrokerDispatch, useBrokerActions };
