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

import { PIPELINE, SERVICES, NODES } from './urls';

const NAVS = [
  {
    testId: 'pipelines-link',
    to: PIPELINE,
    text: 'Pipelines',
    iconCls: 'fa-code-branch',
  },
  {
    testId: 'nodes-link',
    to: NODES,
    text: 'Nodes',
    iconCls: 'fa-sitemap',
  },
  {
    testId: 'services-link',
    to: SERVICES,
    text: 'Services',
    iconCls: 'fa-project-diagram',
  },
  // Disable this nav item for now, monitoring page is not implemented in v0.2
  // {
  //   testId: 'monitoring-link',
  //   to: MONITORING,
  //   text: 'Monitoring',
  //   iconCls: 'fa-desktop',
  // },
];

export default NAVS;
