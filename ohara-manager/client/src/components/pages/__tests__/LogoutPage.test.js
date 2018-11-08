import React from 'react';
import toastr from 'toastr';
import { shallow } from 'enzyme';

import localStorageMock from '__mocks__/localStorage';
import LogoutPage from '../LogoutPage';
import { HOME } from 'constants/urls';
import { LOGOUT_SUCCESS } from 'constants/messages';
import { deleteUserKey } from 'utils/authHelpers';
import { logout } from 'apis/authApis';

window.localStorage = localStorageMock;

jest.mock('axios');
jest.mock('utils/authHelpers');
jest.mock('apis/authApis');

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
