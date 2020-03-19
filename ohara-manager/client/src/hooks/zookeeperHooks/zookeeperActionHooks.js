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

export const useFetchZookeeperAction = () => {
  const dispatch = useDispatch();
  const { zookeeperGroup } = useApp();
  return function(name) {
    const params = { name, group: zookeeperGroup };
    dispatch(actions.fetchZookeeper.trigger(params));
  };
};

export const useCreateZookeeperAction = () => {
  const dispatch = useDispatch();
  const { zookeeperGroup } = useApp();
  return function(values) {
    const finalValues = { ...values, group: zookeeperGroup };
    dispatch(actions.createZookeeper.trigger(finalValues));
  };
};

export const useUpdateZookeeperAction = () => {
  const dispatch = useDispatch();
  const { zookeeperGroup } = useApp();
  return function(values) {
    const finalValues = { ...values, group: zookeeperGroup };
    dispatch(actions.updateZookeeper.trigger(finalValues));
  };
};

export const useStartZookeeperAction = () => {
  const dispatch = useDispatch();
  const { brokerGroup } = useApp();
  return function(name) {
    const params = { name, group: brokerGroup };
    dispatch(actions.startZookeeper.trigger(params));
  };
};

export const useStopZookeeperAction = () => {
  const dispatch = useDispatch();
  const { brokerGroup } = useApp();
  return function(name) {
    const params = { name, group: brokerGroup };
    dispatch(actions.stopZookeeper.trigger(params));
  };
};

export const useDeleteZookeeperAction = () => {
  const dispatch = useDispatch();
  const { zookeeperGroup } = useApp();
  return function(name) {
    const params = { name, group: zookeeperGroup };
    dispatch(actions.deleteZookeeper.trigger(params));
  };
};
