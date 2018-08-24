import React from 'react';
import toastr from 'toastr';
import { Redirect } from 'react-router-dom';

import { logout } from '../../apis/authApis';
import { deleteUserKey } from '../../utils/authHelpers';
import { HOME } from '../../constants/urls';
import { LOGOUT_SUCCESS } from '../../constants/messages';

class LogoutPage extends React.Component {
  state = {
    redirect: false,
  };

  async componentDidMount() {
    const res = await logout();

    if (res) {
      this.setState({ redirect: true });
      this.props.updateLoginState(false);
      deleteUserKey();
      toastr.success(LOGOUT_SUCCESS);
    }
  }

  render() {
    if (this.state.redirect) {
      return <Redirect to={HOME} />;
    }
    return <div>Logging you out…</div>;
  }
}

export default LogoutPage;
