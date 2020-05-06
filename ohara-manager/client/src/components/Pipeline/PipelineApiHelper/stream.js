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
  const updateStreamLink = hooks.useUpdateStreamLinkAction();

  const create = (params, paperApi) => {
    const { className } = params;
    const newParams = { ...params, stream__class: className };
    const options = { paperApi };
    createStream(newParams, options);
  };

  const update = (cell, topics, values, streams, paperApi) => {
    const options = { topics, cell, streams, paperApi };
    updateStream(values, options);
  };

  const updateLinkTo = ({ toStream, topic, link }, paperApi) => {
    const params = {
      name: toStream.name,
      to: [{ name: topic.name }],
    };
    const options = { link, paperApi };
    updateStreamLink(params, options);
  };

  const updateLinkFrom = ({ fromStream, topic, link }, paperApi) => {
    const params = {
      name: fromStream.name,
      from: [{ name: topic.name }],
    };
    const options = { link, paperApi };
    updateStreamLink(params, options);
  };

  const start = ({ id, name }, paperApi) => {
    startStream({ id, name }, { paperApi });
  };

  const stop = ({ id, name }, paperApi) => {
    stopStream({ id, name }, { paperApi });
  };

  const remove = ({ id, name }, paperApi) => {
    deleteStream({ id, name }, { paperApi });
  };

  const removeLinkTo = (params, topic, paperApi) => {
    const newParams = { ...params, to: [] };
    const options = { topic, paperApi };
    removeStreamToLink(newParams, options);
  };

  const removeLinkFrom = (params, topic, paperApi) => {
    const newParams = { ...params, from: [] };
    const options = { topic, paperApi };
    removeStreamFromLink(newParams, options);
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
