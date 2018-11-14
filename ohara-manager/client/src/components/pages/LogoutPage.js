import React from 'react';
import PropTypes from 'prop-types';
import toastr from 'toastr';
import { Redirect } from 'react-router-dom';

import * as _ from 'utils/commonUtils';
import { logout } from 'apis/authApis';
import { deleteUserKey } from 'utils/authUtils';
import { HOME } from 'constants/urls';
import { LOGOUT_SUCCESS } from 'constants/messages';

class LogoutPage extends React.Component {
  static propTypes = {
    updateLoginState: PropTypes.func.isRequired,
  };

  state = {
    redirect: false,
  };

  async componentDidMount() {
    const res = await logout();
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (isSuccess) {
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
    return <div>Logging you out...</div>;
  }
}

export default LogoutPage;
