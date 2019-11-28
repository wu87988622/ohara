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
  fetchZookeepersCreator,
  addZookeeperCreator,
  updateZookeeperCreator,
  deleteZookeeperCreator,
} from './zookeeperActions';
import { reducer, initialState } from './zookeeperReducer';

const ZookeeperStateContext = React.createContext();
const ZookeeperDispatchContext = React.createContext();

const ZookeeperProvider = ({ children }) => {
  const [state, dispatch] = React.useReducer(reducer, initialState);
  const showMessage = useSnackbar();

  const fetchZookeepers = React.useCallback(
    fetchZookeepersCreator(state, dispatch, showMessage),
    [state],
  );

  React.useEffect(() => {
    fetchZookeepers();
  }, [fetchZookeepers]);

  return (
    <ZookeeperStateContext.Provider value={state}>
      <ZookeeperDispatchContext.Provider value={dispatch}>
        {children}
      </ZookeeperDispatchContext.Provider>
    </ZookeeperStateContext.Provider>
  );
};

ZookeeperProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

const useZookeeperState = () => {
  const context = React.useContext(ZookeeperStateContext);
  if (context === undefined) {
    throw new Error(
      'useZookeeperState must be used within a ZookeeperProvider',
    );
  }
  return context;
};

const useZookeeperDispatch = () => {
  const context = React.useContext(ZookeeperDispatchContext);
  if (context === undefined) {
    throw new Error(
      'useZookeeperDispatch must be used within a ZookeeperProvider',
    );
  }
  return context;
};

const useZookeeperActions = () => {
  const state = useZookeeperState();
  const dispatch = useZookeeperDispatch();
  const showMessage = useSnackbar();
  return {
    addZookeeper: addZookeeperCreator(state, dispatch, showMessage),
    updateZookeeper: updateZookeeperCreator(state, dispatch, showMessage),
    deleteZookeeper: deleteZookeeperCreator(state, dispatch, showMessage),
  };
};

export {
  ZookeeperProvider,
  useZookeeperState,
  useZookeeperDispatch,
  useZookeeperActions,
};
