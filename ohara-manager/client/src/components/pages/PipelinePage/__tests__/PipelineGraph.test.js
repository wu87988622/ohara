import React from 'react';
import { shallow } from 'enzyme';

import PipelineGraph from '../PipelineGraph';

const props = {
  iconMaps: {},
  graph: [
    {
      type: 'source',
      uuid: 'uuid1234',
      isActive: false,
      isExist: false,
      icon: 'fa-database',
    },
    {
      type: 'topic',
      uuid: 'uuid5678',
      isActive: false,
      isExist: false,
      icon: '',
    },
    {
      type: 'sink',
      uuid: 'uuid9112',
      isActive: false,
      isExist: false,
      icon: '',
    },
  ],
  resetGraph: jest.fn(),
  updateGraph: jest.fn(),
  match: {},
};

describe('<PipelineGraph />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<PipelineGraph {...props} />);
  });

  it('renders self', () => {
    expect(wrapper).toHaveLength(1);
    expect(wrapper.name()).toBe('Box');
  });

  it('renders <H5Wrapper />', () => {
    expect(wrapper.find('H5Wrapper').length).toBe(1);
    expect(
      wrapper
        .find('H5Wrapper')
        .children()
        .text(),
    ).toBe('Pipeline graph');
  });

  // TODO: complete the tests
});
