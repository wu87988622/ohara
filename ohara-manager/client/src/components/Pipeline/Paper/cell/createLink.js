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

import * as joint from 'jointjs';

const createLink = options => {
  const { sourceId, targetId } = options;

  const link = new joint.shapes.standard.Link();
  link.source({ id: sourceId });
  link.target({ id: targetId });
  link.attr({ line: { stroke: '#9e9e9e' } });
  return link;
};

export default createLink;
