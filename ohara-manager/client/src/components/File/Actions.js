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
import Action from './Action';

function Actions(props) {
  if (props?.actions) {
    return props.actions.map((action, index) => (
      <Action
        action={action}
        data={props?.data}
        disabled={props?.disabled}
        key={`action-${index}`}
      />
    ));
  }
  return null;
}

Actions.propTypes = {
  actions: PropTypes.array.isRequired,
  data: PropTypes.object,
  disabled: PropTypes.bool,
};

Actions.defaultProps = {
  actions: [],
  data: {},
  disabled: false,
};

export default Actions;
