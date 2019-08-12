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
import { CONNECTOR_STATES } from 'constants/pipelines';

describe('getCurrTopicId()', () => {
  it('gets the right topic id', () => {
    const originals = generate.topics({ count: 3 });

    // set the target to the randomly generated topics
    const target = originals[2];

    const topic = utils.getCurrTopicId({ originals, target: target.name });
    expect(topic).toBe(target.name);
  });

  it('returns an empty array if param originals is empty', () => {
    const originals = [];
    const target = generate.name();

    const result = utils.getCurrTopicId({ originals, target });
    expect(Array.isArray(result)).toBe(true);
  });

  it('returns any empty array if param target is null', () => {
    const originals = generate.topics({ count: 2 });
    const target = null;

    const result = utils.getCurrTopicId({ originals, target });
    expect(Array.isArray(result)).toBe(true);
  });

  it('returns an empty array if the given target is == `Please select...`', () => {
    const originals = generate.topics({ count: 2 });
    const target = 'Please select...';

    const result = utils.getCurrTopicId({ originals, target });
    expect(Array.isArray(result)).toBe(true);
  });
});

describe('addColumn()', () => {
  it('adds a new column to empty column array', () => {
    const configs = { columns: [] };
    const parentValues = { name: generate.name() };
    const newColumn = generate.columnRows(1);

    const update = utils.addColumn({
      configs,
      update: { ...newColumn[0], parentValues },
    });

    const expected = {
      columns: [
        {
          order: 1,
          name: newColumn[0].columnName,
          newName: newColumn[0].newColumnName,
          dataType: newColumn[0].currType,
        },
      ],
      ...parentValues,
    };
    expect(update).toEqual(expected);
  });

  it('adds a new column to an existing column array', () => {
    const columns = generate.columnRows(2);
    const otherKey = generate.number();
    const configs = { columns, otherKey };
    const parentValues = { name: generate.name() };

    const newColumn = generate.columnRows(1);
    const update = utils.addColumn({
      configs,
      update: { ...newColumn[0], parentValues },
    });

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
      ...parentValues,
    };

    expect(update).toEqual(expected);
  });
});

describe('getEditable()', () => {
  it('return true if the key includes `name` or `connector_class`', () => {
    expect(utils.getEditable({ key: 'name', defaultEditable: true })).toBe(
      false,
    );

    expect(
      utils.getEditable({ key: 'connector_class', defaultEditable: true }),
    ).toBe(false);
  });

  it('returns the default key if the key is not matched', () => {
    expect(
      utils.getEditable({ key: 'someOtherKey', defaultEditable: true }),
    ).toBe(true);

    expect(utils.getEditable({ key: 'nah', defaultEditable: false })).toBe(
      false,
    );
  });
});

describe('getDisplayValue()', () => {
  it('uses defaultValue if configValue is not given', () => {
    const defaultValue = generate.id();
    expect(utils.getDisplayValue({ defaultValue })).toBe(defaultValue);
  });

  it(`uses configValue instead of defaultValue if these is one`, () => {
    const configValue = generate.id();
    const defaultValue = generate.id();
    expect(utils.getDisplayValue({ configValue, defaultValue })).toBe(
      configValue,
    );
  });
});

describe('changeKeySeparator()', () => {
  it('change key separator from `.` to `_`', () => {
    const key = 'abc.efg';
    expect(utils.changeKeySeparator(key)).toBe('abc_efg');
  });

  it('change key separator from `_` to `.`', () => {
    const key = 'abc_efg_no_oh';
    expect(utils.changeKeySeparator(key)).toBe('abc.efg.no.oh');
  });
});

describe('sortByOrder', () => {
  it('sorts out the definition by the property: `orderInGroup`', () => {
    const defs = [
      {
        name: 'b',
        orderInGroup: 2,
      },
      {
        name: 'c',
        orderInGroup: 3,
      },
      {
        name: 'a',
        orderInGroup: 1,
      },
    ];

    const expected = [
      {
        name: 'a',
        orderInGroup: 1,
      },
      {
        name: 'b',
        orderInGroup: 2,
      },
      {
        name: 'c',
        orderInGroup: 3,
      },
    ];

    expect(defs.sort(utils.sortByOrder)).toEqual(expected);
  });
});

describe('getDisplayValue()', () => {
  it('returns config value if it contains a value', () => {
    const params = { configValue: 'abc', defaultValue: 2 };
    expect(utils.getDisplayValue(params)).toBe(params.configValue);
  });

  it(`returns defalut value if configValue doesn't contain any value`, () => {
    const params = { configValue: null, defaultValue: 10 };
    expect(utils.getDisplayValue(params)).toBe(params.defaultValue);
  });

  // Edge case, see the getDisplayValue function comments
  it('handles `Boolean` values correctly', () => {
    const params = { configValue: true, defaultValue: false };
    expect(utils.getDisplayValue(params)).toBe(params.configValue);
  });
});

describe('getRenderData()', () => {
  it('gets the correct format of render data', () => {
    const state = CONNECTOR_STATES.running;
    const configs = {
      name: generate.name(),
    };

    const defs = [
      {
        key: 'abc.123',
        orderInGroup: 2,
        defaultValue: 123,
      },
      {
        key: 'abc.456',
        orderInGroup: 3,
        defaultValue: 456,
      },
      {
        key: 'abc.876',
        orderInGroup: 1,
        defaultValue: 876,
      },
    ];

    const expected = [
      {
        orderInGroup: 1,
        key: 'abc_876',
        displayValue: 876,
        defaultValue: 876,
        isRunning: true,
      },
      {
        orderInGroup: 2,
        key: 'abc_123',
        displayValue: 123,
        defaultValue: 123,
        isRunning: true,
      },
      {
        orderInGroup: 3,
        key: 'abc_456',
        displayValue: 456,
        defaultValue: 456,
        isRunning: true,
      },
    ];

    const result = utils.getRenderData({ state, defs, configs });
    expect(result).toEqual(expected);
  });
});

describe('switchType()', () => {
  it('returns `text` if the given type is `STRING`', () => {
    expect(utils.switchType('STRING')).toBe('text');
  });

  it('returns `number` if the given type is `INT`', () => {
    expect(utils.switchType('INT')).toBe('number');
  });

  it('returns `PASSWORD` if the given type is `password`', () => {
    expect(utils.switchType('PASSWORD')).toBe('password');
  });

  it(`returns null if there's no match`, () => {
    expect(utils.switchType('')).toBe(null);
    expect(utils.switchType(null)).toBe(null);
    expect(utils.switchType(undefined)).toBe(null);
  });
});

describe('changeToken()', () => {
  it('replace token `.` with `_`', () => {
    const values = {
      'abc.efg.dfx.dfs': 'abcefg',
      'nope.nah.sdkl.ljdsf': 'nopenah',
    };

    const expected = {
      abc_efg_dfx_dfs: 'abcefg',
      nope_nah_sdkl_ljdsf: 'nopenah',
    };

    expect(
      utils.changeToken({ values, targetToken: '.', replaceToken: '_' }),
    ).toEqual(expected);
  });

  it('replace token `` with `.`', () => {
    const values = {
      qwe_kjlksd_kjlsjf_dfs: 'sdfxdf',
      nope_nah: 'sdf',
    };

    const expected = {
      'qwe.kjlksd.kjlsjf.dfs': 'sdfxdf',
      'nope.nah': 'sdf',
    };

    expect(
      utils.changeToken({ values, targetToken: '_', replaceToken: '.' }),
    ).toEqual(expected);
  });
});

describe('groupBy()', () => {
  it('returns grouped items', () => {
    const json = [
      { key: 'a', name: 'Name1' },
      { key: 'a', name: 'Name2' },
      { key: 'a', name: 'Name3' },
      { key: 'b', name: 'Name4' },
      { key: 'b', name: 'Name5' },
      { key: 'b', name: 'Name6' },
    ];

    const groupJson = [
      [
        { key: 'a', name: 'Name1' },
        { key: 'a', name: 'Name2' },
        { key: 'a', name: 'Name3' },
      ],
      [
        { key: 'b', name: 'Name4' },
        { key: 'b', name: 'Name5' },
        { key: 'b', name: 'Name6' },
      ],
    ];

    const group = utils.groupBy(json, item => {
      return [item.key];
    });

    expect(group).toEqual(groupJson);
  });
});
