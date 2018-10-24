import React from 'react';
import { shallow } from 'enzyme';

import Toolbar from '../Toolbar';
import { getTestById } from 'utils/testHelpers';

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
  iconKeys: {
    jdbcSource: 'com.island.ohara.connector.jdbc.JDBCSourceConnector',
    ftpSource: 'com.island.ohara.connector.ftp.FtpSource',
    hdfsSink: 'com.island.ohara.connector.hdfs.HDFSSinkConnector',
  },
};

describe('<Toolbar />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<Toolbar {...props} />);
  });

  it('renders self', () => {
    expect(wrapper).toHaveLength(1);
    expect(wrapper.name()).toBe('ToolbarWrapper');
  });

  it('renders database icon', () => {
    const db = wrapper.find(getTestById('toolbar-source'));
    expect(db.length).toBe(1);
    expect(db.prop('data-id')).toBe(props.iconKeys.jdbcSource);
  });

  it('renders hadoop icon', () => {
    const hadoop = wrapper.find(getTestById('toolbar-sink'));
    expect(hadoop.length).toBe(1);
    expect(hadoop.prop('data-id')).toBe(props.iconKeys.hdfsSink);
    expect(hadoop.find('HadoopIconWrapper').length).toBe(1);
  });

  it('renders topic icon', () => {
    const db = wrapper.find(getTestById('toolbar-topic'));
    expect(db.length).toBe(1);
    expect(db.prop('data-id')).toBe('topic');
  });
});
