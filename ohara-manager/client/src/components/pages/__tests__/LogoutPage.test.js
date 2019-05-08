/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from 'react';
import toastr from 'toastr';
import { shallow } from 'enzyme';

import localStorageMock from '__mocks__/localStorage';
import LogoutPage from '../LogoutPage';
import { HOME } from 'constants/urls';
import { LOGOUT_SUCCESS } from 'constants/messages';
import { deleteUserKey } from 'utils/authUtils';
import { logout } from 'api/authApi';

window.localStorage = localStorageMock;

jest.mock('api/apiUtils');
jest.mock('utils/authUtils');
jest.mock('api/authApi');

const props = {
  updateLoginState: jest.fn(),
};

describe('<LogoutPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<LogoutPage {...props} />);
  });

  it('should do nothing if log out failed', async () => {
    const res = { data: { isSuccess: false } };
    logout.mockImplementation(() => Promise.resolve(res));

    wrapper = await shallow(<LogoutPage {...props} />);

    expect(props.updateLoginState).toHaveBeenCalledTimes(0);
    expect(toastr.success).toHaveBeenCalledTimes(0);
    expect(deleteUserKey).toHaveBeenCalledTimes(0);
    expect(wrapper.children().text()).toBe('Logging you out...');
  });

  it('should redirect to homepage if log out succeeded', async () => {
    const res = { data: { isSuccess: true } };
    logout.mockImplementation(() => Promise.resolve(res));

    wrapper = await shallow(<LogoutPage {...props} />);

    expect(props.updateLoginState).toHaveBeenCalledTimes(1);
    expect(props.updateLoginState).toHaveBeenCalledWith(false);
    expect(toastr.success).toHaveBeenCalledTimes(1);
    expect(toastr.success).toHaveBeenCalledWith(LOGOUT_SUCCESS);
    expect(deleteUserKey).toHaveBeenCalledTimes(1);

    expect(wrapper.name()).toBe('Redirect');
    expect(wrapper.props().to).toBe(HOME);
  });
});
