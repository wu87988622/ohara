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

export const addPipelineStatus = (pipelines = []) => {
  const updatedPipeline = pipelines.reduce((acc, pipeline) => {
    const { objects } = pipeline;
    const status = objects.filter(object => Boolean(object.state));

    let updatedStatus = '';
    const connectorsAreRunning =
      status.length !== 0 && status.length >= objects.length;

    connectorsAreRunning
      ? (updatedStatus = 'Running')
      : (updatedStatus = 'Stopped');

    return [
      ...acc,
      {
        ...pipeline,
        status: updatedStatus,
      },
    ];
  }, []);

  return updatedPipeline;
};