import React from 'react';
import { shallow } from 'enzyme';

import PipelineToolbar from '../PipelineToolbar';
import { ICON_KEYS } from 'constants/pipelines';
import { getTestById } from 'utils/testUtils';
import { fetchCluster } from 'utils/pipelineToolbarUtils';

jest.mock('utils/pipelineToolbarUtils');

const props = {
  match: {
    isExact: false,
    params: {},
    path: 'test/path',
    url: 'test/url',
  },
  graph: [
    {
      type: 'source',
      id: '1',
      isActive: false,
      isExact: false,
      icon: 'fa-test',
    },
  ],
  updateGraph: jest.fn(),
  hasChanges: false,
  iconMaps: {},
  iconKeys: ICON_KEYS,
};

describe('<PipelineToolbar />', () => {
  let wrapper;
  beforeEach(() => {
    fetchCluster.mockImplementation(() =>
      Promise.resolve({ sources: [], sinks: [] }),
    );

    wrapper = shallow(<PipelineToolbar {...props} />);
  });

  it('renders self', () => {
    expect(wrapper).toHaveLength(1);
    expect(wrapper.name()).toBe('ToolbarWrapper');
  });

  it('renders <FileSavingStatus />', () => {
    expect(wrapper.find('FileSavingStatus').length).toBe(1);
  });

  it('renders the correct status base on this.props.hasChanges', () => {
    expect(
      wrapper
        .find('FileSavingStatus')
        .children()
        .text(),
    ).toBe('All changes saved');

    wrapper = shallow(<PipelineToolbar {...props} hasChanges={true} />);
    wrapper.update();
    expect(
      wrapper
        .find('FileSavingStatus')
        .children()
        .text(),
    ).toBe('Saving...');
  });

  it('renders sources icon', () => {
    expect(wrapper.find(getTestById('toolbar-sources')).length).toBe(1);
  });

  it('renders topic icon', () => {
    expect(wrapper.find(getTestById('toolbar-topics')).length).toBe(1);
  });

  it('renders sink icon', () => {
    expect(wrapper.find(getTestById('toolbar-sinks')).length).toBe(1);
  });

  it('renders streams icon', () => {
    expect(wrapper.find(getTestById('toolbar-streams')).length).toBe(1);
  });
});
