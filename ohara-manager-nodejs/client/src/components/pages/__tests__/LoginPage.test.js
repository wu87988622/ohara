import React from 'react';
import toastr from 'toastr';
import { Redirect } from 'react-router-dom';
import { shallow } from 'enzyme';

import localStorageMock from '../../../__mocks__/localStorage';

import LoginPage from '../LoginPage';
import * as LOGIN_PAGE from '../../../constants/login';
import * as api from '../../../apis/authApi';

window.localStorage = localStorageMock;

api.login = jest.fn(() => {
  return { data: { token: 'toekn' }, isSuccess: true };
});

jest.mock('toastr', () => {
  return {
    success: jest.fn(),
  };
});

describe('<LoginPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<LoginPage />);
  });

  it('renders self', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.hasClass('form-container')).toBe(true);
  });

  it('renders <h3 />', () => {
    const h3 = wrapper.find('h3');

    expect(h3.length).toBe(1);
    expect(h3.hasClass('h3')).toBe(true);
    expect(h3.text()).toBe(LOGIN_PAGE.PAGE_HEADING);
  });

  it('renders <UserInput />', () => {
    const userInput = wrapper.find('#username');
    const _props = userInput.props();

    expect(userInput.length).toBe(1);
    expect(_props.id).toBe('username');
    expect(_props.className).toBe('form-control');
    expect(_props.type).toBe('text');
    expect(_props.placeholder).toBe(LOGIN_PAGE.USERNAME_PLACEHOLDER);
    expect(_props.value).toBe('');
    expect(_props.onChange).toBeDefined();
  });

  it('renders <PasswordInput />', () => {
    const passwordInput = wrapper.find('#password');
    const _props = passwordInput.props();

    expect(passwordInput.length).toBe(1);
    expect(_props.id).toBe('password');
    expect(_props.className).toBe('form-control');
    expect(_props.type).toBe('password');
    expect(_props.placeholder).toBe(LOGIN_PAGE.PASSWORD_PLACEHOLDER);
    expect(_props.value).toBe('');
    expect(_props.onChange).toBeDefined();
  });

  it('renders <button />', () => {
    const btn = wrapper.find('button[type="submit"]');
    const _props = btn.props();
    expect(btn.length).toBe(1);
    expect(_props.type).toBe('submit');
    expect(_props.className).toBe('btn btn-lg btn-primary btn-block');
    expect(btn.text()).toBe(LOGIN_PAGE.SUBMIT_BUTTON_TEXT);
  });

  it('calls handleChange() on <UserInput />', () => {
    const evt = {
      target: {
        value: 'test',
        id: 'username',
      },
    };

    wrapper.find('#username').simulate('change', evt);
    expect(wrapper.find('#username').props().value).toBe(evt.target.value);
  });

  it('calls handleChange() on <PasswordInput />', () => {
    const evt = {
      target: {
        value: 'test',
        id: 'password',
      },
    };

    wrapper.find('#password').simulate('change', evt);
    expect(wrapper.find('#password').props().value).toBe(evt.target.value);
  });

  it('calls onSubmit()', () => {
    const evt = { preventDefault: jest.fn() };
    expect(api.login).toHaveBeenCalledTimes(0);
    wrapper.setState({ username: 'test', password: '1234' });
    wrapper.find('.form-login').simulate('submit', evt);
    expect(api.login).toHaveBeenCalledTimes(1);
  });

  it('renders <Redirect /> if the this.state.redirect is true', () => {
    expect(wrapper.dive().type()).toBe('div');

    wrapper.setState({ redirect: true });

    expect(wrapper.type()).toBe(Redirect);
  });
});
