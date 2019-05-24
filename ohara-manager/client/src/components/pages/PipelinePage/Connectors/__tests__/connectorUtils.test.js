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

import * as generate from 'utils/generate';
import * as utils from '../connectorUtils';

describe('updateConfigs()', () => {
  it('updates configs', () => {
    const configs = {};
    const name = generate.name();
    const value = generate.message();
    const target = {
      name,
      value,
    };

    const update = utils.updateConfigs({ configs, target });
    expect(update).toEqual({ [name]: value });
  });
});

describe('getCurrTopicId()', () => {
  it('gets the right topic id', () => {
    const originals = generate.topics(3);

    // set the target to the randomly generated topics
    const target = originals[2];

    const id = utils.getCurrTopicId({ originals, target: target.name });
    expect(id).toBe(target.id);
  });

  it(`return undefined if param originals is empty`, () => {
    const originals = [];
    const target = generate.name();

    const id = utils.getCurrTopicId({ originals, target });
    expect(id).toBeUndefined();
  });

  it(`return undefined if param target is empty`, () => {
    const originals = generate.topics(2);
    const target = '';

    const id = utils.getCurrTopicId({ originals, target });
    expect(id).toBeUndefined();
  });
});

describe('getCurrTopicName()', () => {
  it('gets the right topic name', () => {
    const originals = generate.topics(3);
    const targetTopic = originals[1];
    const target = [targetTopic.id];

    const topicName = utils.getCurrTopicName({ originals, target });
    expect(topicName).toBe(targetTopic.name);
  });
});

describe('addColumn()', () => {
  it('adds a new column to empty column array', () => {
    const configs = { columns: [] };

    const newColumn = generate.columnRows(1);

    const update = utils.addColumn({ configs, newColumn: newColumn[0] });
    const expected = {
      columns: [
        {
          order: 1,
          name: newColumn[0].columnName,
          newName: newColumn[0].newColumnName,
          dataType: newColumn[0].currType,
        },
      ],
    };
    expect(update).toEqual(expected);
  });

  it('adds a new column to an existing column array', () => {
    const columns = generate.columnRows(2);

    const otherKey = generate.number();

    const configs = { columns, otherKey };

    const newColumn = generate.columnRows(1);
    const update = utils.addColumn({ configs, newColumn: newColumn[0] });

    const expected = {
      columns: [
        ...columns,
        {
          order: 2,
          name: newColumn[0].columnName,
          newName: newColumn[0].newColumnName,
          dataType: newColumn[0].currType,
        },
      ],
      otherKey,
    };

    expect(update).toEqual(expected);
  });
});
