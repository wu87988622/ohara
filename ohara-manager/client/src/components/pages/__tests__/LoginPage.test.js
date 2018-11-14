import React from 'react';
import toastr from 'toastr';
import { shallow } from 'enzyme';

import * as LOGIN_PAGE from 'constants/login';
import localStorageMock from '__mocks__/localStorage';
import LoginPage from '../LoginPage';
import { setUserKey } from 'utils/authUtils';
import { login } from 'apis/authApis';
import { LOGIN_SUCCESS } from 'constants/messages';
import { HOME } from 'constants/urls';
import { LOGIN } from 'constants/documentTitles';

window.localStorage = localStorageMock;

jest.mock('apis/authApis');
jest.mock('utils/authUtils');

const props = {
  updateLoginState: jest.fn(),
};

describe('<LoginPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<LoginPage {...props} />);
  });

  it('renders self', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.dive().name()).toBe('DocumentTitle');
  });

  it('renders <DocumentTitle />', () => {
    expect(wrapper.dive().props().title).toBe(LOGIN);
  });

  it('renders <Heading3 />', () => {
    const heading3 = wrapper.find('Heading3');
    expect(heading3.length).toBe(1);
    expect(heading3.children().text()).toBe(LOGIN_PAGE.PAGE_HEADING);
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
    expect(_props.name).toBe('username');
    expect(_props.placeholder).toBe(LOGIN_PAGE.USERNAME_PLACEHOLDER);
  });

  it('renders <PasswordInput />', () => {
    const input = wrapper.find('PasswordInput');
    const _props = input.props();
    expect(input.length).toBe(1);
    expect(_props.value).toBe('');
    expect(_props.type).toBe('password');
    expect(_props.name).toBe('password');
    expect(_props.placeholder).toBe(LOGIN_PAGE.PASSWORD_PLACEHOLDER);
  });

  it('renders <Button />', () => {
    const btn = wrapper.find('Button');
    const _props = btn.props();
    expect(btn.length).toBe(1);
    expect(_props.text).toBe(LOGIN_PAGE.SUBMIT_BUTTON_TEXT);
  });

  it('should do nothing if log in failed', async () => {
    const res = { data: { isSuccess: false } };
    const evt = { preventDefault: jest.fn() };

    login.mockImplementation(() => Promise.resolve(res));

    await wrapper.find('Form').simulate('submit', evt);
    expect(evt.preventDefault).toHaveBeenCalledTimes(1);
    expect(setUserKey).toHaveBeenCalledTimes(0);
    expect(props.updateLoginState).toHaveBeenCalledTimes(0);
    expect(toastr.success).toHaveBeenCalledTimes(0);

    expect(wrapper.dive().name()).toBe('DocumentTitle');
  });

  it('should redirect to homepage if successfully logged in the user', async () => {
    const res = { data: { isSuccess: true, token: '123456' } };
    const evt = { preventDefault: jest.fn() };
    const loginInfo = { username: 'abc', password: '123' };

    login.mockImplementation(() => Promise.resolve(res));

    wrapper.setState({ ...loginInfo });
    await wrapper.find('Form').simulate('submit', evt);

    expect(evt.preventDefault).toHaveBeenCalledTimes(1);
    expect(login).toHaveBeenCalledWith(loginInfo);
    expect(setUserKey).toHaveBeenCalledTimes(1);
    expect(setUserKey).toHaveBeenCalledWith(res.data.token);
    expect(props.updateLoginState).toHaveBeenCalledTimes(1);
    expect(props.updateLoginState).toHaveBeenCalledWith(true);
    expect(toastr.success).toHaveBeenCalledTimes(1);
    expect(toastr.success).toHaveBeenCalledWith(LOGIN_SUCCESS);

    expect(wrapper.name()).toBe('Redirect');
    expect(wrapper.props().to).toBe(HOME);
  });
});
