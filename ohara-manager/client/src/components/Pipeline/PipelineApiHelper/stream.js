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

const stream = () => {
  const updateStream = hooks.useUpdateStreamAction();
  const createStream = hooks.useCreateStreamAction();
  const deleteStream = hooks.useDeleteStreamAction();
  const startStream = hooks.useStartStreamAction();
  const stopStream = hooks.useStopStreamAction();
  const removeStreamToLink = hooks.useRemoveStreamToLinkAction();
  const removeStreamFromLink = hooks.useRemoveStreamFromLinkAction();
  const updateStreamFromLink = hooks.useUpdateStreamFromLinkAction();
  const updateStreamToLink = hooks.useUpdateStreamToLinkAction();
  const currentStreams = hooks.useStreams();

  const create = (params, paperApi) => {
    const { id, name, className, jarKey } = params;
    const values = { name, stream__class: className, jarKey };
    const options = { id, paperApi };
    createStream(values, options);
  };

  const update = (cell, topics, values, paperApi) => {
    const options = { topics, cell, currentStreams, paperApi };
    updateStream(values, options);
  };

  const updateLinkTo = ({ toStream, topic, link }, paperApi) => {
    const params = {
      name: toStream.name,
      to: [{ name: topic.name }],
    };
    const options = { link, paperApi };
    updateStreamToLink(params, options);
  };

  const updateLinkFrom = ({ fromStream, topic, link }, paperApi) => {
    const params = {
      name: fromStream.name,
      from: [{ name: topic.name }],
    };
    const options = { link, paperApi };
    updateStreamFromLink(params, options);
  };

  const start = (params, paperApi) => {
    const { id, name } = params;
    startStream(name, { id, paperApi });
  };

  const stop = (params, paperApi) => {
    const { id, name } = params;
    stopStream(name, { id, paperApi });
  };

  const remove = (params, paperApi) => {
    const { id, name } = params;
    deleteStream(name, { id, paperApi });
  };

  const removeLinkTo = ({ id, name }, topic, paperApi) => {
    const params = { name, to: [] };
    const options = { id, topic, paperApi };
    removeStreamToLink(params, options);
  };

  const removeLinkFrom = ({ id, name }, topic, paperApi) => {
    const params = { name, from: [] };
    const options = { id, topic, paperApi };
    removeStreamFromLink(params, options);
  };

  return {
    create,
    update,
    updateLinkTo,
    updateLinkFrom,
    start,
    stop,
    remove,
    removeLinkTo,
    removeLinkFrom,
  };
};

export default stream;
