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

import { useCallback, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';

import * as hooks from 'hooks';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';

export const useIsNodeLoaded = () => {
  const mapState = useCallback(state => !!state.ui.node?.lastUpdated, []);
  return useSelector(mapState);
};

export const useIsNodeLoading = () => {
  const mapState = useCallback(state => !!state.ui.node?.loading, []);
  return useSelector(mapState);
};

export const useAllNodes = () => {
  const fetchNodes = useCallback(hooks.useFetchNodesAction(), []);
  const isLoaded = hooks.useIsNodeLoaded();
  useEffect(() => {
    if (!isLoaded) fetchNodes();
  }, [fetchNodes, isLoaded]);

  return useSelector(state => selectors.getAllNodes(state));
};

export const useCreateNodeAction = () => {
  const dispatch = useDispatch();
  return function(values) {
    dispatch(actions.createNode.trigger(values));
  };
};

export const useUpdateNodeAction = () => {
  const dispatch = useDispatch();
  return function(values) {
    dispatch(actions.updateNode.trigger(values));
  };
};

export const useFetchNodesAction = () => {
  const dispatch = useDispatch();
  return function() {
    dispatch(actions.fetchNodes.trigger());
  };
};

export const useDeleteNodeAction = () => {
  const dispatch = useDispatch();
  return function(values) {
    dispatch(actions.deleteNode.trigger(values));
  };
};
