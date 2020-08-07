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

import moment from 'moment';
import _ from 'lodash';
import { ofType } from 'redux-observable';
import { defer, from, queueScheduler } from 'rxjs';
import { concatMap, exhaustMap } from 'rxjs/operators';

import { KIND, LOG_TIME_GROUP, GROUP } from 'const';
import * as logApi from 'api/logApi';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { catchErrorWithEventLog } from '../utils';

export default (action$, state$) =>
  action$.pipe(
    ofType(actions.fetchDevToolLog.TRIGGER),
    exhaustMap(() =>
      defer(() => {
        const getDevToolLog = selectors.makeGetDevToolLog();
        const workspaceName = selectors.getWorkspaceName(state$.value);
        const zkId = () => ({
          name: workspaceName,
          group: GROUP.ZOOKEEPER,
        });
        const bkId = () => ({
          name: workspaceName,
          group: GROUP.BROKER,
        });
        const wkId = () => ({
          name: workspaceName,
          group: GROUP.WORKER,
        });
        const log = getDevToolLog(state$.value);
        const {
          logType,
          shabondiKey,
          streamKey,
          timeGroup,
          timeRange,
          startTime,
          endTime,
        } = log.query;
        const getTimeSeconds = () => {
          if (timeGroup === LOG_TIME_GROUP.latest) {
            // timeRange uses minute units
            return timeRange * 60;
          } else {
            return Math.ceil(
              moment
                .duration(moment(endTime).diff(moment(startTime)))
                .asSeconds(),
            );
          }
        };

        switch (logType) {
          case KIND.configurator:
            return logApi.getConfiguratorLog({
              sinceSeconds: getTimeSeconds(),
            });
          case KIND.zookeeper:
            return logApi.getZookeeperLog(zkId(), {
              sinceSeconds: getTimeSeconds(),
            });
          case KIND.broker:
            return logApi.getBrokerLog(bkId(), {
              sinceSeconds: getTimeSeconds(),
            });
          case KIND.worker:
            return logApi.getWorkerLog(wkId(), {
              sinceSeconds: getTimeSeconds(),
            });
          case KIND.shabondi:
            return logApi.getShabondiLog(shabondiKey, {
              sinceSeconds: getTimeSeconds(),
            });
          case KIND.stream:
            return logApi.getStreamLog(streamKey, {
              sinceSeconds: getTimeSeconds(),
            });
          default:
            throw new Error('Unsupported logType');
        }
      }).pipe(
        concatMap((res) =>
          from(
            [
              actions.fetchDevToolLog.success(
                _.map(res.data.logs, (log) => ({
                  name: log.hostname,
                  logs: _.split(log.value, '\n'),
                })),
              ),
              // we set the hostname of dropdown list to first host of log data response
              actions.setDevToolLogQueryParams.success({
                hostName: _.get(res.data, 'logs[0].hostname', ''),
              }),
            ],
            queueScheduler,
          ),
        ),
        catchErrorWithEventLog((err) => actions.fetchDevToolLog.failure(err)),
      ),
    ),
  );
