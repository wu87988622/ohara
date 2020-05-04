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

import { noop } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import * as actions from 'store/actions';
import deletePipelineEpic from '../../pipeline/deletePipelineEpic';
import { getId } from 'utils/object';
import { KIND, CELL_STATUS } from 'const';
import { entity as pipelineEntity } from 'api/__mocks__/pipelineApi';
import { entity as connectorEntity } from 'api/__mocks__/connectorApi';
import { entity as streamEntity } from 'api/__mocks__/streamApi';
import { entity as topicEntity } from 'api/__mocks__/topicApi';

jest.mock('api/pipelineApi');
jest.mock('api/connectorApi');
jest.mock('api/streamApi');
jest.mock('api/topicApi');

const mockedPaperApi = jest.fn(() => {
  return {
    updateElement: () => noop(),
    removeElement: () => noop(),
  };
});

const paperApi = new mockedPaperApi();
const cells = [
  {
    kind: KIND.stream,
    name: streamEntity.name,
    group: streamEntity.group,
    status: CELL_STATUS.stopped,
  },
  {
    kind: KIND.source,
    name: connectorEntity.name,
    group: connectorEntity.group,
    status: CELL_STATUS.stopped,
  },
  {
    kind: KIND.topic,
    name: topicEntity.name,
    group: topicEntity.group,
    status: CELL_STATUS.running,
  },
];

const makeTestScheduler = () =>
  new TestScheduler((actual, expected) => {
    expect(actual).toEqual(expected);
  });

it('should delete a pipeline', () => {
  makeTestScheduler().run(helpers => {
    const { hot, expectObservable, expectSubscriptions, flush } = helpers;

    const input = '   ^-a                                                ';
    const expected = '-- 1s a 999ms b 9ms c 499ms d 999ms (ef) 496ms (gh)';
    const subs = '    ^--------------------------------------------------';

    const action$ = hot(input, {
      a: {
        type: actions.deletePipeline.TRIGGER,
        payload: {
          params: {
            name: pipelineEntity.name,
            group: pipelineEntity.group,
            cells,
          },
          options: { paperApi },
        },
      },
    });
    const output$ = deletePipelineEpic(action$);

    expectObservable(output$).toBe(expected, {
      a: {
        type: actions.deleteConnector.SUCCESS,
        payload: {
          name: connectorEntity.name,
          group: connectorEntity.group,
        },
      },
      b: {
        type: actions.deleteStream.SUCCESS,
        payload: {
          name: streamEntity.name,
          group: streamEntity.group,
        },
      },
      c: {
        type: actions.stopTopic.REQUEST,
        payload: {
          kind: KIND.topic,
          name: topicEntity.name,
          group: topicEntity.group,
          status: CELL_STATUS.running,
        },
      },
      d: {
        type: actions.stopTopic.SUCCESS,
        payload: topicEntity,
      },
      e: {
        type: actions.deleteTopic.SUCCESS,
        payload: {
          name: topicEntity.name,
          group: topicEntity.group,
        },
      },
      f: {
        type: actions.deletePipeline.REQUEST,
      },
      g: {
        type: actions.deletePipeline.SUCCESS,
        payload: getId(pipelineEntity),
      },
      h: {
        type: actions.switchPipeline.TRIGGER,
      },
    });

    expectSubscriptions(action$.subscriptions).toBe(subs);

    flush();
  });
});
