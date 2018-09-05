import React from 'react';
import { shallow } from 'enzyme';

import PipelineGraph from '../PipelineGraph';

const props = {
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

  it('renders <Graph />', () => {
    expect(wrapper.find('Graph').length).toBe(1);
  });

  it('renders <Node />', () => {
    const nodes = wrapper.find('Node');
    expect(nodes.length).toBe(3);
    nodes.forEach((n, i) => {
      expect(n.props()['data-id']).toBe(props.graph[i].type);
    });
  });
});
