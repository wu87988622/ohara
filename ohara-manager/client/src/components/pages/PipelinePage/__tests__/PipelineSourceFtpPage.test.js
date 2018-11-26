import React from 'react';
import { shallow } from 'enzyme';

import PipelineSourceFtp from '../PipelineSourceFtpPage';

const props = {
  hasChanges: false,
  updateHasChanges: jest.fn(),
  updateGraph: jest.fn(),
  loadGraph: jest.fn(),
  match: {},
  schema: [],
};

describe('<PipelineSourceFtp />', () => {
  let wrapper;

  beforeEach(() => {
    wrapper = shallow(<PipelineSourceFtp {...props} />);
  });

  it('renders self', () => {
    expect(wrapper.length).toBe(1);
  });

  it('renders <NewRowBtn />', () => {
    expect(wrapper.find('NewRowBtn').length).toBe(1);
    expect(wrapper.find('NewRowBtn').props().text).toBe('New row');
  });

  it('renders <Modal />', () => {
    const modal = wrapper.find('Modal');
    expect(modal.length).toBe(1);
    expect(modal.props().isActive).toBe(false);
  });

  it('renders <ConfirmModal />', () => {
    expect(wrapper.find('ConfirmModal').length).toBe(1);
    expect(wrapper.find('ConfirmModal').props().title).toBe('Delete row?');
    expect(wrapper.find('ConfirmModal').props().isActive).toBe(false);
  });

  it('calls handleDeleteRowModalOpen', () => {
    const evt = { preventDefault: jest.fn() };
    wrapper.instance().handleDeleteRowModalOpen(evt);
    expect(wrapper.find('ConfirmModal').props().isActive).toBe(true);
  });

  it('calls handleDeleteRowModalClose', () => {
    wrapper.instance().handleDeleteRowModalClose();
    expect(wrapper.find('ConfirmModal').props().isActive).toBe(false);
  });

  it('calls handleTypeChange', () => {
    const evt = { persist: jest.fn(), target: { value: 'integer' } };
    wrapper.instance().handleTypeChange(evt);
    expect(wrapper.state().schema[0].dataType).toBe('integer');
  });

  it('calls handleRowDelete', () => {
    wrapper.setState({
      workingRow: 1,
      schema: [{ order: 0 }, { order: 1 }, { order: 2 }],
    });
    wrapper.instance().handleRowDelete();
    expect(wrapper.state().schema.length).toBe(2);
    expect(wrapper.state().isDeleteRowModalActive).toBe(false);
  });

  it('calls handleNewModalOpen', () => {
    wrapper.instance().handleNewRowModalOpen();
    expect(wrapper.state().isNewRowModalActive).toBe(true);
    expect(wrapper.state().currType).toBe('string');
  });

  it('calls handleNewModalClose', () => {
    wrapper.instance().handleNewRowModalClose();
    const {
      isNewRowModalActive,
      currType,
      columnName,
      newColumnName,
    } = wrapper.state();

    expect(isNewRowModalActive).toBe(false);
    expect(currType).toBe('');
    expect(columnName).toBe('');
    expect(newColumnName).toBe('');
  });

  it('calls handleRowCreate', () => {
    wrapper.setState({
      schema: [],
      columnName: 'column1',
      newColumnName: 'COLUMN1',
      currType: 'string',
    });
    wrapper.instance().handleRowCreate();
    wrapper.setState({
      schema: wrapper.state().schema,
      columnName: 'column2',
      newColumnName: 'COLUMN2',
      currType: 'integer',
    });
    wrapper.instance().handleRowCreate();
    expect(wrapper.state().schema.length).toBe(2);
  });

  it('test handleUp', () => {
    wrapper.setState({
      schema: [
        {
          columnName: 'column1',
          newColumnName: 'COLUMN1',
          currType: 'string',
          order: 1,
        },
        {
          columnName: 'column2',
          newColumnName: 'COLUMN2',
          currType: 'boolean',
          order: 2,
        },
      ],
    });
    const evt = { preventDefault: jest.fn() };
    wrapper.instance().handleUp(evt, 2);

    const { schema } = wrapper.state();
    expect(schema[0].columnName).toBe('column2');
    expect(schema[0].newColumnName).toBe('COLUMN2');
    expect(schema[0].currType).toBe('boolean');
    expect(schema[0].order).toBe(1);

    expect(schema[1].columnName).toBe('column1');
    expect(schema[1].newColumnName).toBe('COLUMN1');
    expect(schema[1].currType).toBe('string');
    expect(schema[1].order).toBe(2);
  });

  it('test handleDown', () => {
    wrapper.setState({
      schema: [
        {
          columnName: 'column1',
          newColumnName: 'COLUMN1',
          currType: 'string',
          order: 1,
        },
        {
          columnName: 'column2',
          newColumnName: 'COLUMN2',
          currType: 'boolean',
          order: 2,
        },
      ],
    });
    const evt = { preventDefault: jest.fn() };
    wrapper.instance().handleDown(evt, 1);

    const { schema } = wrapper.state();

    expect(schema[0].columnName).toBe('column2');
    expect(schema[0].newColumnName).toBe('COLUMN2');
    expect(schema[0].currType).toBe('boolean');
    expect(schema[0].order).toBe(1);

    expect(schema[1].columnName).toBe('column1');
    expect(schema[1].newColumnName).toBe('COLUMN1');
    expect(schema[1].currType).toBe('string');
    expect(schema[1].order).toBe(2);
  });
});
