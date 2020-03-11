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

import { useReducer } from 'react';
import { useLocalStorage } from 'utils/hooks';

export const usePipelineState = () => {
  const [isMetricsOn, setIsMetricsOn] = useLocalStorage(
    'isPipelineMetricsOn',
    false,
  );

  const initialState = {
    isToolboxOpen: true,
    toolboxKey: 0,
    isMetricsOn,
    toolboxExpanded: {
      topic: false,
      source: false,
      sink: false,
      stream: false,
    },
  };

  const reducer = (state, action) => {
    const { type, payload } = action;
    switch (type) {
      case 'openToolbox':
        return {
          ...state,
          isToolboxOpen: true,
        };

      case 'closeToolbox':
        return {
          ...state,
          isToolboxOpen: false,
          toolboxExpanded: initialState.toolboxExpanded,
        };

      case 'setToolbox':
        return {
          ...state,
          toolboxExpanded: {
            ...state.toolboxExpanded,
            [payload]: !state.toolboxExpanded[payload],
          },
        };

      case 'setMultiplePanels':
        return {
          ...state,
          toolboxExpanded: {
            ...state.toolboxExpanded,
            ...payload,
          },
        };

      case 'setToolboxKey':
        return {
          ...state,
          toolboxKey: state.toolboxKey + 1,
        };

      case 'resetToolbox':
        return {
          ...state,
          toolboxExpanded: initialState.toolboxExpanded,
        };

      case 'toggleMetricsButton':
        setIsMetricsOn(!state.isMetricsOn);
        return {
          ...state,
          isMetricsOn: !state.isMetricsOn,
        };
      case 'resetPipeline':
        return initialState;
      default:
        return state;
    }
  };

  return useReducer(reducer, initialState);
};
