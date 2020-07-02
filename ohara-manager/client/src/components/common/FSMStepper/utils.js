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

import { LOG_TYPES, STEP_STAGES } from './const';

export const createLog = ({
  title,
  type = LOG_TYPES.EVENT,
  stepStage = STEP_STAGES.LOADING,
  payload,
  isRevert = false,
}) => {
  const now = new Date();
  const key = now.getTime().toString();
  return {
    title,
    type,
    stepStage,
    payload,
    isRevert,
    key,
    createdAt: now,
  };
};
