import React from 'react';
import { shallow } from 'enzyme';

import { ListTable } from '../Table';

const props = {
  headers: ['a', 'b'],
  list: [{ a: 'abcde' }, { b: 'fijkl' }],
  urlDir: 'testDir',
};

// TODO: skip the test for now
describe.skip('<ListTable />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<ListTable {...props} />);
  });

  it('renders self', () => {
    expect(wrapper).toHaveLength(1);
    expect(wrapper.dive().type()).toBe('table');
  });

  it('renders <Th /> correctly', () => {
    const ths = wrapper.find('Th');
    expect(ths.length).toBe(props.headers.length);
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
