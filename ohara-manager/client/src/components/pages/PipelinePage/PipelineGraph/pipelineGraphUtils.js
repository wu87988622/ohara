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

import { CONNECTOR_STATES } from 'constants/pipelines';
import { get } from 'lodash';
import { isEmptyStr } from 'utils/commonUtils';
import {
  isSource,
  isSink,
  isTopic,
  isStream,
} from '../pipelineUtils/commonUtils';

export const getIcon = kind => {
  let icon = '';

  if (isSource(kind)) {
    icon = 'fa-file-import';
  } else if (isSink(kind)) {
    icon = 'fa-file-export';
  } else if (isTopic(kind)) {
    icon = 'fa-list-ul';
  } else if (isStream(kind)) {
    icon = 'fa-wind';
  }

  return icon;
};

export const getStatusIcon = state => {
  let icon = '';

  if (state === CONNECTOR_STATES.failed) {
    icon = 'fa-exclamation-circle';
  }

  return icon;
};

export const createHtml = params => {
  const {
    kind,
    state,
    metrics,
    name,
    workerClusterName,
    className,
    isActive,
  } = params;

  const activeClass = isActive ? 'is-active' : '';
  const connectorState = state ? state : '';
  const numberOfRows = get(metrics, 'meters[0].value', 0);
  const sizeOfRows = get(metrics, 'meters[1].value', 0);
  const topicClass = kind === 'topic' ? 'node-topic' : 'node-connector';
  const stateClass = !isEmptyStr(connectorState)
    ? `is-${connectorState.toLowerCase()}`
    : '';
  const status = !isEmptyStr(connectorState)
    ? connectorState.toLowerCase()
    : 'stopped';
  const icon = getIcon(kind);
  const statusIcon = getStatusIcon(connectorState);
  const displayKind = className.split('.').pop();

  const html = `<div class="node-graph ${topicClass} ${stateClass} ${activeClass}">
        <div class="basic-info">
          <span class="node-icon"><i class="fa ${icon}"></i></span>
          <div class="node-text-wrapper">
            <span class="node-name">${name}</span>
            <span class="node-status">Status: ${status}</span>
            <span class="node-type">${displayKind}</span>
          </div>
        </div>
        <div class="metrics">
          <span class="metrics-icon"><i class="fas fa-tachometer-alt"></i></span>
           <div class="metrics-text-wrapper">
            <span class="number-of-rows">${numberOfRows}</span>
            <span class="size-of-rows">${sizeOfRows}</span>
          </div>
        </div>
        <a class="status-icon" href="/logs/workers/${workerClusterName}" target="_blank">
          <i class="fas ${statusIcon}"></i>
        </a>
      </div>`;

  return html;
};
