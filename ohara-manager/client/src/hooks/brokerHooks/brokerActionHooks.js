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

import { useDispatch } from 'redux-react-hook';
import { useApp } from 'context';
import * as actions from 'store/actions';

export const useFetchBrokerAction = () => {
  const dispatch = useDispatch();
  const { brokerGroup } = useApp();
  return function(name) {
    const params = { name, group: brokerGroup };
    dispatch(actions.fetchBroker.trigger(params));
  };
};

export const useCreateBrokerAction = () => {
  const dispatch = useDispatch();
  const { brokerGroup } = useApp();
  return function(values) {
    const finalValues = { ...values, group: brokerGroup };
    dispatch(actions.createBroker.trigger(finalValues));
  };
};

export const useUpdateBrokerAction = () => {
  const dispatch = useDispatch();
  const { brokerGroup } = useApp();
  return function(values) {
    const finalValues = { ...values, group: brokerGroup };
    dispatch(actions.updateBroker.trigger(finalValues));
  };
};

export const useStartBrokerAction = () => {
  const dispatch = useDispatch();
  const { brokerGroup } = useApp();
  return function(name) {
    const params = { name, group: brokerGroup };
    dispatch(actions.startBroker.trigger(params));
  };
};

export const useStopBrokerAction = () => {
  const dispatch = useDispatch();
  const { brokerGroup } = useApp();
  return function(name) {
    const params = { name, group: brokerGroup };
    dispatch(actions.stopBroker.trigger(params));
  };
};

export const useDeleteBrokerAction = () => {
  const dispatch = useDispatch();
  const { brokerGroup } = useApp();
  return function(name) {
    const params = { name, group: brokerGroup };
    dispatch(actions.deleteBroker.trigger(params));
  };
};
