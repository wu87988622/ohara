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

import { isEmpty } from 'lodash';

import { CONNECTOR_STATES } from 'constants/pipelines';
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

const createMetricsHtml = meters => {
  const meterData = meters
    .map(meter => `<span>${meter.value.toLocaleString()}</span>`)
    .join('');
  const metricsHtml = `<div class="metrics">
    <span class="metrics-icon"><i class="fas fa-tachometer-alt"></i></span>
      <div class="metrics-text-wrapper">
        ${meterData}
      </div>
  </div>
  `;

  return metricsHtml;
};

export const createHtml = params => {
  const {
    kind,
    state,
    name,
    workerClusterName,
    className = 'streamApp', // temp fix for now
    isActive,
    metrics = {},
  } = params;

  // The metrics data are not ready yet, add default value so
  // it won't break our app
  const { meters = [] } = metrics;

  const activeClass = isActive ? 'is-active' : '';
  const connectorState = state ? state : '';
  const isTopic = kind === 'topic';

  const topicClass = isTopic ? 'node-topic' : 'node-connector';
  const stateClass = !isEmptyStr(connectorState)
    ? `is-${connectorState.toLowerCase()}`
    : '';
  const status = !isEmptyStr(connectorState)
    ? connectorState.toLowerCase()
    : 'stopped';
  const icon = getIcon(kind);
  const statusIcon = getStatusIcon(connectorState);
  const displayKind = className.split('.').pop();
  const metricsHtml = createMetricsHtml(meters);

  const html = `<div class="node-graph ${topicClass} ${stateClass} ${activeClass}">
        <div class="basic-info">
          <span class="node-icon"><i class="fa ${icon}"></i></span>
          <div class="node-text-wrapper">
            <span class="node-name">${name}</span>
            <span class="node-status">Status: ${status}</span>
            <span class="node-type">${displayKind}</span>
          </div>
        </div>
        <a class="status-icon" href="/logs/workers/${workerClusterName}" target="_blank">
          <i class="fas ${statusIcon}"></i>
        </a>
        ${!isEmpty(meters) ? metricsHtml : ''}
      </div>`;

  return html;
};
