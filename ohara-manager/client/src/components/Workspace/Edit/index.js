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

import EditWorkspace from './EditWorkspace';

const Tabs = {
  OVERVIEW: 'overview',
  TOPICS: 'topics',
  FILES: 'files',
  AUTOFILL: 'autofill',
  SETTINGS: 'settings',
};

const SubTabs = {
  SETTINGS: 'settings',
  PLUGINS: 'plugins',
  NODES: 'nodes',
  NONE: 'none',
};

const Segments = {
  WORKER: 'worker',
  BROKER: 'broker',
  ZOOKEEPER: 'zookeeper',
  NONE: 'none',
};

export { EditWorkspace, Tabs, SubTabs, Segments };
