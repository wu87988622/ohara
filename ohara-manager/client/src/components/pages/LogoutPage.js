import React from 'react';
import toastr from 'toastr';
import { Redirect } from 'react-router-dom';

import { logout } from '../../apis/authApis';
import { deleteUserKey } from '../../utils/authHelpers';
import { HOME } from '../../constants/url';
import { LOGOUT_SUCCESS } from '../../constants/message';

class LogoutPage extends React.Component {
  state = {
    redirect: false,
  };

  async componentDidMount() {
    const res = await logout();

    if (res) {
      this.setState({ redirect: true });
      deleteUserKey();
      toastr.success(LOGOUT_SUCCESS);
    }
  }

  render() {
    if (this.state.redirect) {
      return <Redirect to={HOME} />;
    }
    return <div>登出中…</div>;
  }
}

export default LogoutPage;
