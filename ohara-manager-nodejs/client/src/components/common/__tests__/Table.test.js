import React from 'react';
import { shallow } from 'enzyme';

import Table from '../Table';

const props = {
  headers: ['a', 'b'],
  list: [{ a: 'abcde' }, { b: 'fijkl' }],
  urlDir: 'testDir',
};

describe('<Table />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<Table {...props} />);
  });

  test('renders self', () => {
    expect(wrapper).toHaveLength(1);
    expect(wrapper.hasClass('table table-striped')).toBe(true);
    expect(wrapper.type()).toBe('table');
  });

  it('renders <th /> correctly', () => {
    const ths = wrapper.find('th');
    expect(ths.length).toBe(props.headers.length);
    expect(ths.at(0).text()).toBe(props.headers[0]);
    expect(ths.at(1).text()).toBe(props.headers[1]);
  });

  // TODO: finish up the test
  it('renders <td /> correctly', () => {
    const trs = wrapper.find('tbody > tr');
    expect(trs.length).toBe(props.list.length);

    const firstRow = trs.at(0).find('td');
    const secondRow = trs.at(1).find('td');

    expect(
      firstRow
        .find('td')
        .at(0)
        .text(),
    ).toBe(props.list[0].a);

    expect(
      secondRow
        .find('td')
        .at(0)
        .text(),
    ).toBe(props.list[1].b);
  });
});
