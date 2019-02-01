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
import PropTypes from 'prop-types';
import toastr from 'toastr';
import { Redirect } from 'react-router-dom';
import { get } from 'lodash';

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
    const isSuccess = get(res, 'data.isSuccess', false);

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
