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
import { get, includes, uniq, pull, union } from 'lodash';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Switch from '@material-ui/core/Switch';

import { KIND } from 'const';
import {
  useWorkerActions,
  useBrokerActions,
  useZookeeperActions,
} from 'context';
import Badge from 'components/common/Badge';

const ServiceSwitch = ({ cluster, nodeName, type }) => {
  const { stageWorker } = useWorkerActions();
  const { stageBroker } = useBrokerActions();
  const { stageZookeeper } = useZookeeperActions();

  if (!cluster) return <></>;

  const currentValue = get(cluster, 'nodeNames', []);
  const stagingValue = get(cluster, 'stagingSettings.nodeNames', []);

  const value = union(stagingValue, currentValue);
  const initialValue = currentValue;
  const checked = includes(value, nodeName);
  const initialChecked = includes(initialValue, nodeName);
  const dirty = checked !== initialChecked;

  const handleChange = ({ target }) => {
    const nodeNames = target.checked
      ? uniq([...value, nodeName])
      : pull(value, nodeName);

    const {
      settings: { name, group },
    } = cluster;

    const data = { name, group, nodeNames };
    if (type === KIND.worker) stageWorker(data);
    if (type === KIND.broker) stageBroker(data);
    if (type === KIND.zookeeper) stageZookeeper(data);
  };

  return (
    <>
      <FormControlLabel
        value={type}
        control={
          <Switch color="primary" checked={checked} onChange={handleChange} />
        }
        label={
          <Badge variant="dot" invisible={!dirty} color="warning">
            {type}
          </Badge>
        }
        labelPlacement="bottom"
      />
    </>
  );
};

ServiceSwitch.propTypes = {
  nodeName: PropTypes.string.isRequired,
  cluster: PropTypes.shape({
    settings: PropTypes.shape({
      name: PropTypes.string.isRequired,
      group: PropTypes.string.isRequired,
      nodeName: PropTypes.array,
    }).isRequired,
    stagingSettings: PropTypes.shape({
      nodeName: PropTypes.array,
    }),
  }),
  type: PropTypes.string.isRequired,
};

export default ServiceSwitch;
