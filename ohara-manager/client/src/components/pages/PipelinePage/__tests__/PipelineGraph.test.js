import React from 'react';
import { shallow } from 'enzyme';

import PipelineGraph from '../PipelineGraph';

const props = {
  iconMaps: {},
  graph: [
    {
      kind: 'source',
      id: 'id1234',
      isActive: false,
      isExist: false,
      icon: 'fa-database',
    },
    {
      kind: 'topic',
      id: 'id5678',
      isActive: false,
      isExist: false,
      icon: '',
    },
    {
      kind: 'sink',
      id: 'id9112',
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

  it('renders <Svg />', () => {
    const svg = wrapper.find('Svg');
    expect(svg.length).toBe(1);
    expect(svg.hasClass('pipeline-graph')).toBe(true);
  });
});
