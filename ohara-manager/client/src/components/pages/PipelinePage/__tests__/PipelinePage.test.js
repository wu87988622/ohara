import React from 'react';
import { shallow } from 'enzyme';

import PipelinePage from '../PipelinePage';

describe('<PipelinePage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<PipelinePage />);
  });

  it('renders correctly', () => {
    expect(wrapper.length).toBe(1);
  });

  it('renders <H2 />', () => {
    expect(wrapper.find('H2').length).toBe(1);
  });

  it('renders <NewPipelineBtn>', () => {
    expect(wrapper.find('NewPipelineBtn').length).toBe(1);
    expect(wrapper.find('NewPipelineBtn').props().text).toBe('New pipeline');
  });

  it('renders <Modal />', () => {
    const modal = wrapper.find('Modal');
    expect(modal.length).toBe(1);
    expect(modal.props().isActive).toBe(false);
  });

  it('opens the <Modal /> with <NewPipelineBtn />', () => {
    const evt = { preventDefault: jest.fn() };
    expect(wrapper.find('Modal').props().isActive).toBe(false);
    wrapper
      .find('NewPipelineBtn')
      .dive()
      .dive()
      .simulate('click', evt);
    wrapper.update();
    expect(wrapper.find('Modal').props().isActive).toBe(true);
  });

  it('closes the <Modal /> with <CloseBtn />', () => {
    wrapper.setState({ isModalActive: true });
    expect(wrapper.find('Modal').props().isActive).toBe(true);

    wrapper
      .find('Modal')
      .dive()
      .find('CloseBtn')
      .simulate('click');

    wrapper.update();
    expect(wrapper.find('Modal').props().isActive).toBe(false);
  });

  it('closes the <Modal> with <CancelBtn />', () => {
    wrapper.setState({ isModalActive: true });
    expect(wrapper.find('Modal').props().isActive).toBe(true);

    wrapper
      .find('Modal')
      .dive()
      .find('[data-testid="modal-cancel-btn"]')
      .dive()
      .dive()
      .simulate('click');

    wrapper.update();
    expect(wrapper.find('Modal').props().isActive).toBe(false);
  });

  it('toggles disabled props to <Modal /> based on this.state.topic length', () => {
    expect(wrapper.find('Modal').props().isConfirmDisabled).toBe(true);
    wrapper.setState({ topics: [{ name: 'test', uuid: '2' }] });
    expect(wrapper.find('Modal').props().isConfirmDisabled).toBe(false);
  });

  it('renders <DataTable />', () => {
    const data = [
      { name: 'a', status: 'stopped', uuid: '1' },
      { name: 'b', status: 'start', uuid: '2' },
    ];
    wrapper.setState({ pipelines: data });

    const table = wrapper.find('DataTable');
    const _props = table.props();

    expect(table.length).toBe(1);
    expect(_props.align).toBe('center');

    const trs = table.find('tr');
    expect(trs.length).toBe(data.length);

    const rows = table.find('tr');

    const firstRow = rows.at(0).find('td');
    const secondRow = rows.at(1).find('td');

    const expected = [
      ['0', data[0].name, data[0].status],
      ['1', data[1].name, data[1].status],
    ];

    firstRow.forEach((x, idx) => {
      if (!x.props().className) {
        expect(x.text()).toBe(expected[0][idx]);
      }
    });

    expect(firstRow.find('.has-icon').length).toBe(3);

    secondRow.forEach((x, idx) => {
      if (!x.props().className) {
        expect(x.text()).toBe(expected[1][idx]);
      }
    });

    expect(secondRow.find('.has-icon').length).toBe(3);
  });
});
