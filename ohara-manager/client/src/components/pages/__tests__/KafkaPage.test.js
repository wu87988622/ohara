import React from 'react';
import { shallow } from 'enzyme';

import KafkaPage from '../KafkaPage';
import { LEAVE_WITHOUT_SAVE } from '../../../constants/messages';

describe('<KafkaPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<KafkaPage />);
  });

  it('renders correctly', () => {
    expect(wrapper.length).toBe(1);
  });

  it('renders <Modal />', () => {
    const modal = wrapper.find('Modal');
    const _props = modal.props();
    expect(modal.length).toBe(1);
    expect(_props.isActive).toBe(false);
  });

  it('opens <Modal />', () => {
    expect(wrapper.find('Modal').props().isActive).toBe(false);
    wrapper
      .find('[data-testid="new-topic"]')
      .dive()
      .simulate('click');

    wrapper.update();
    expect(wrapper.find('Modal').props().isActive).toBe(true);
  });

  it.only('closes <Modal /> with <CloseBtn>', () => {
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

  it('closes <Modal> with <CancelBtn>', () => {
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

  it('renders <Prompt />', () => {
    const prompt = wrapper.find('Prompt');
    const _props = prompt.props();
    expect(prompt.length).toBe(1);
    expect(_props.when).toBe(false);
    expect(_props.message).toBe(LEAVE_WITHOUT_SAVE);
  });

  it('shows a <Prompt /> when the form has been edited', () => {
    expect(wrapper.find('Prompt').props().when).toBe(false);
    wrapper.setState({ isFormDirty: true });
    expect(wrapper.find('Prompt').props().when).toBe(true);
  });
});
