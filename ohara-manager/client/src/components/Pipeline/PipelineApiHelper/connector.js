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

const connector = () => {
  const createConnector = hooks.useCreateConnectorAction();
  const startConnector = hooks.useStartConnectorAction();
  const stopConnector = hooks.useStopConnectorAction();
  const deleteConnector = hooks.useDeleteConnectorAction();
  const updateConnector = hooks.useUpdateConnectorAction();
  const updateConnectorLink = hooks.useUpdateConnectorLinkAction();
  const removeConnectorSourceLink = hooks.useRemoveSourceLinkAction();
  const removeConnectorSinkLink = hooks.useRemoveSinkLinkAction();
  const workerClusterKey = hooks.useWorkerClusterKey();
  const topicGroup = hooks.useTopicGroup();

  const create = async (values, paperApi) => {
    const { id, name, className } = values;
    const params = { id, name, connector__class: className, workerClusterKey };
    const options = { paperApi };
    createConnector(params, options);
  };

  const update = async (cell, topics, values, paperApi) => {
    const params = { name: cell.name, ...values };
    const options = { paperApi, cell, topics };
    updateConnector(params, options);
  };

  const start = async (values, paperApi) => {
    const { id, name } = values;
    const params = { id, name };
    const options = { paperApi };
    startConnector(params, options);
  };

  const stop = async (values, paperApi) => {
    const { id, name } = values;
    const params = { id, name };
    const options = { paperApi };
    stopConnector(params, options);
  };

  const remove = async (values, paperApi) => {
    const { id, name } = values;
    const params = { id, name };
    const options = { paperApi };
    deleteConnector(params, options);
  };

  const updateLink = async (values, paperApi) => {
    const { connector, topic, link } = values;
    const params = {
      name: connector.name,
      topicKeys: [{ name: topic.name, group: topicGroup }],
    };
    const options = { link, paperApi };
    updateConnectorLink(params, options);
  };

  const removeSourceLink = async (values, topic, paperApi) => {
    const { name, id } = values;
    const params = { name, id, topicKeys: [] };
    const options = { topic, paperApi };
    removeConnectorSourceLink(params, options);
  };

  const removeSinkLink = async (values, topic, paperApi) => {
    const { name, id } = values;
    const params = { name, id, topicKeys: [] };
    const options = { topic, paperApi };
    removeConnectorSinkLink(params, options);
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
