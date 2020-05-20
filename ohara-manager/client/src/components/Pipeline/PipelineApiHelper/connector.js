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

import * as hooks from 'hooks';
import { port } from 'utils/generate';
import { KIND } from 'const';
import { isShabondi } from '../PipelineUtils';

const connector = () => {
  const createConnector = hooks.useCreateConnectorAction();
  const createShabondi = hooks.useCreateShabondiAction();
  const startConnector = hooks.useStartConnectorAction();
  const startShabondi = hooks.useStartShabondiAction();
  const stopConnector = hooks.useStopConnectorAction();
  const stopShabondi = hooks.useStopShabondiAction();
  const deleteConnector = hooks.useDeleteConnectorAction();
  const deleteShabondi = hooks.useDeleteShabondiAction();
  const updateConnector = hooks.useUpdateConnectorAction();
  const updateShabondi = hooks.useUpdateShabondiAction();
  const updateConnectorLink = hooks.useUpdateConnectorLinkAction();
  const updateShabondiLink = hooks.useUpdateShabondiLinkAction();
  const removeConnectorSourceLink = hooks.useRemoveSourceLinkAction();
  const removeShabondiSourceLink = hooks.useRemoveShabondiSourceLinkAction();
  const removeConnectorSinkLink = hooks.useRemoveSinkLinkAction();
  const removeShabondiSinkLink = hooks.useRemoveShabondiSinkLinkAction();
  const brokerClusterKey = {
    name: hooks.useBrokerName(),
    group: hooks.useBrokerGroup(),
  };
  const workerClusterKey = hooks.useWorkerClusterKey();
  const topicGroup = hooks.useTopicGroup();

  const create = (values, paperApi) => {
    const { id, name, className } = values;
    const params = { id, name };
    const options = { paperApi };
    isShabondi(className)
      ? createShabondi(
          {
            ...params,
            // shabondi requires client port
            // we randomize a default value and follow the backend rule that port > 1024
            shabondi__client__port: port({ min: 1025 }),
            shabondi__class: className,
            brokerClusterKey,
          },
          options,
        )
      : createConnector(
          { ...params, connector__class: className, workerClusterKey },
          options,
        );
  };

  const update = (cell, topics, values, connectors, paperApi) => {
    const params = { name: cell.name, ...values };
    const options = { cell, topics, connectors, paperApi };
    isShabondi(cell.className)
      ? updateShabondi(params, options)
      : updateConnector(params, options);
  };

  const start = (values, paperApi) => {
    const { id, name, className } = values;
    const params = { id, name };
    const options = { paperApi };
    isShabondi(className)
      ? startShabondi(params, options)
      : startConnector(params, options);
  };

  const stop = (values, paperApi) => {
    const { id, name, className } = values;
    const params = { id, name };
    const options = { paperApi };
    isShabondi(className)
      ? stopShabondi(params, options)
      : stopConnector(params, options);
  };

  const remove = (values, paperApi) => {
    const { id, name, className } = values;
    const params = { id, name };
    const options = { paperApi };
    isShabondi(className)
      ? deleteShabondi(params, options)
      : deleteConnector(params, options);
  };

  const updateLink = (values, paperApi) => {
    const { connector, topic, link } = values;
    const topicKey = { name: topic.name, group: topicGroup };
    const params = {
      name: connector.name,
    };
    const options = { link, paperApi };
    if (isShabondi(connector.className)) {
      if (connector.kind === KIND.sink) {
        updateShabondiLink(
          { ...params, shabondi__sink__fromTopics: [topicKey] },
          options,
        );
      } else {
        updateShabondiLink(
          { ...params, shabondi__source__toTopics: [topicKey] },
          options,
        );
      }
    } else {
      updateConnectorLink({ ...params, topicKeys: [topicKey] }, options);
    }
  };

  const removeSourceLink = (values, topic, paperApi) => {
    const { name, id, className } = values;
    const params = { name, id };
    const options = { topic, paperApi };
    isShabondi(className)
      ? removeShabondiSourceLink(
          { ...params, shabondi__source__toTopics: [] },
          options,
        )
      : removeConnectorSourceLink({ ...params, topicKeys: [] }, options);
  };

  const removeSinkLink = (values, topic, paperApi) => {
    const { name, id, className } = values;
    const params = { name, id };
    const options = { topic, paperApi };
    isShabondi(className)
      ? removeShabondiSinkLink(
          { ...params, shabondi__sink__fromTopics: [] },
          options,
        )
      : removeConnectorSinkLink({ ...params, topicKeys: [] }, options);
  };

  return {
    create,
    update,
    start,
    stop,
    remove,
    updateLink,
    removeSourceLink,
    removeSinkLink,
  };
};

export default connector;
