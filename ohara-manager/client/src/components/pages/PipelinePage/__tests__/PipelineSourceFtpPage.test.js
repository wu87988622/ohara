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

  it('renders correctly', () => {
    expect(wrapper.length).toBe(1);
  });

  it('renders <SchemaBtn />', () => {
    expect(wrapper.find('SchemaBtn').length).toBe(1);
    expect(wrapper.find('SchemaBtn').props().text).toBe('New schema');
  });

  it('renders <Modal />', () => {
    const modal = wrapper.find('Modal');
    expect(modal.length).toBe(1);
    expect(modal.props().isActive).toBe(false);
  });

  it('renders <ConfirmModal />', () => {
    expect(wrapper.find('ConfirmModal').length).toBe(1);
    expect(wrapper.find('ConfirmModal').props().title).toBe('Delete schema?');
    expect(wrapper.find('ConfirmModal').props().isActive).toBe(false);
  });

  it('test handleDeleteSchemaModalOpen', () => {
    const evt = { preventDefault: jest.fn() };
    wrapper.instance().handleDeleteSchemaModalOpen(evt);
    expect(wrapper.find('ConfirmModal').props().isActive).toBe(true);
  });

  it('test handleDeleteSchemaModalClose', () => {
    wrapper.instance().handleDeleteSchemaModalClose();
    expect(wrapper.find('ConfirmModal').props().isActive).toBe(false);
  });

  it('test handleTypeChange', () => {
    const evt = { persist: jest.fn(), target: { value: 'integer' } };
    wrapper.instance().handleTypeChange(evt);
    expect(wrapper.state().schema[0].dataType).toBe('integer');
  });

  it('test handleSchemaDelete', () => {
    wrapper.setState({
      workingRow: 1,
      schema: [{ order: 0 }, { order: 1 }, { order: 2 }],
    });
    wrapper.instance().handleSchemaDelete();
    expect(wrapper.state().schema.length).toBe(2);
    expect(wrapper.state().isDeleteSchemaModalActive).toBe(false);
  });

  it('test handleNewSchemaModalOpen', () => {
    wrapper.instance().handleNewSchemaModalOpen();
    expect(wrapper.state().isNewSchemaModalActive).toBe(true);
    expect(wrapper.state().currType).toBe('string');
  });

  it('test handleNewSchemaModalClose', () => {
    wrapper.instance().handleNewSchemaModalClose();
    const {
      isNewSchemaModalActive,
      currType,
      columnName,
      newColumnName,
    } = wrapper.state();

    expect(isNewSchemaModalActive).toBe(false);
    expect(currType).toBe('');
    expect(columnName).toBe('');
    expect(newColumnName).toBe('');
  });

  it('test handleSchemaCreate', () => {
    wrapper.setState({
      schema: [],
      columnName: 'column1',
      newColumnName: 'COLUMN1',
      currType: 'string',
    });
    wrapper.instance().handleSchemaCreate();
    wrapper.setState({
      schema: wrapper.state().schema,
      columnName: 'column2',
      newColumnName: 'COLUMN2',
      currType: 'integer',
    });
    wrapper.instance().handleSchemaCreate();
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
