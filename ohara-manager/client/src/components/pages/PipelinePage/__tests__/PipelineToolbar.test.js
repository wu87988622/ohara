import React from 'react';
import { shallow } from 'enzyme';

import PipelineToolbar from '../PipelineToolbar';
import { ICON_KEYS } from 'constants/pipelines';
import { getTestById } from 'utils/testUtils';

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
      uuid: '1',
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

  it('renders JDBC source icon', () => {
    expect(wrapper.find(getTestById('toolbar-source')).length).toBe(1);
  });

  it('renders FTP source icon', () => {
    expect(wrapper.find(getTestById('toolbar-source-ftp')).length).toBe(1);
  });

  it('renders HDFS sink icon', () => {
    expect(wrapper.find(getTestById('toolbar-sink')).length).toBe(1);
  });

  it('renders FTP sink icon', () => {
    expect(wrapper.find(getTestById('toolbar-source-ftp')).length).toBe(1);
  });

  it('renders topic icon', () => {
    expect(wrapper.find(getTestById('toolbar-topic')).length).toBe(1);
  });
});
