import React from 'react';
import { Redirect } from 'react-router-dom';
import { shallow } from 'enzyme';

import localStorageMock from '../../../__mocks__/localStorage';

import LoginPage from '../LoginPage';
import * as LOGIN_PAGE from '../../../constants/login';
import {
  PAGE_HEADING,
  USERNAME_PLACEHOLDER,
  PASSWORD_PLACEHOLDER,
} from '../../../constants/login';

window.localStorage = localStorageMock;

describe('<LoginPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<LoginPage />);
  });

  it('renders self', () => {
    expect(wrapper.length).toBe(1);
  });

  it('renders <Heading3 />', () => {
    const heading3 = wrapper.find('Heading3');
    expect(heading3.length).toBe(1);
    expect(heading3.children().text()).toBe(PAGE_HEADING);
  });

  it('renders <Form />', () => {
    const form = wrapper.find('Form');
    expect(form.length).toBe(1);
  });

  it('renders <UsernameInput />', () => {
    const input = wrapper.find('UsernameInput');
    const _props = input.props();
    expect(input.length).toBe(1);
    expect(_props.value).toBe('');
    expect(_props.type).toBe('text');
    expect(_props.placeholder).toBe(USERNAME_PLACEHOLDER);
  });

  it('renders <PasswordInput />', () => {
    const input = wrapper.find('PasswordInput');
    const _props = input.props();
    expect(input.length).toBe(1);
    expect(_props.value).toBe('');
    expect(_props.type).toBe('password');
    expect(_props.placeholder).toBe(PASSWORD_PLACEHOLDER);
  });

  it('renders <Button />', () => {
    const btn = wrapper.find('Button');
    const _props = btn.props();
    expect(btn.length).toBe(1);
    expect(_props.text).toBe(LOGIN_PAGE.SUBMIT_BUTTON_TEXT);
  });

  it('renders <Redirect /> if the this.state.isRedirect is true', () => {
    expect(wrapper.dive().type()).not.toBe(Redirect);
    wrapper.setState({ isRedirect: true });
    expect(wrapper.type()).toBe(Redirect);
  });
});
