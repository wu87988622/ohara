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

  it('renders <Button>', () => {
    expect(wrapper.find('Button').length).toBe(1);
    expect(wrapper.find('Button').props().text).toBe('New pipeline');
  });

  it('renders <Modal />', () => {
    const modal = wrapper.find('Modal');
    expect(modal.length).toBe(1);
    expect(modal.props().isActive).toBe(false);
  });

  it('opens the <Modal /> with <Button />', () => {
    const evt = { preventDefault: jest.fn() };

    expect(wrapper.find('Modal').props().isActive).toBe(false);

    wrapper
      .find('Button')
      .dive()
      .simulate('click', evt);

    wrapper.update();
    expect(wrapper.find('Modal').props().isActive).toBe(true);
  });

  it('closes the <Modal /> with <CloseButton />', () => {
    wrapper.setState({ isModalActive: true });
    expect(wrapper.find('Modal').props().isActive).toBe(true);

    wrapper
      .find('Modal')
      .dive()
      .find('CloseButton')
      .simulate('click');

    wrapper.update();
    expect(wrapper.find('Modal').props().isActive).toBe(false);
  });

  it('closes the <Modal> with <CancelButton />', () => {
    wrapper.setState({ isModalActive: true });
    expect(wrapper.find('Modal').props().isActive).toBe(true);

    wrapper
      .find('Modal')
      .dive()
      .find('[data-testid="modal-cancel-button"]')
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
});
