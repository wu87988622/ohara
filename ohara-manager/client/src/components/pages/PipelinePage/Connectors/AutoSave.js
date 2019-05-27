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
import diff from 'deep-diff';
import { FormSpy } from 'react-final-form';
import { debounce } from 'lodash';

import { changeToken } from './connectorUtils';

class AutoSave extends React.Component {
  static propTypes = {
    values: PropTypes.object.isRequired,
    save: PropTypes.func.isRequired,
    updateHasChanges: PropTypes.func.isRequired,
    hasToken: PropTypes.bool,
  };

  state = {
    values: this.props.values,
  };

  componentDidUpdate() {
    this.save();
  }

  save = debounce(async () => {
    const { values, save, updateHasChanges, hasToken } = this.props;
    const difference = diff(this.state.values, values);

    if (difference && difference.length) {
      // values have changed
      updateHasChanges(true);
      this.setState({ values });

      let _values;
      if (hasToken) {
        _values = changeToken({
          values,
          targetToken: '_',
          replaceToken: '.',
        });
      } else {
        _values = values;
      }

      await save(_values);
      updateHasChanges(false);
    }
  }, 1000);

  render() {
    return null;
  }
}

// Make a HOC
export default props => (
  <FormSpy {...props} subscription={{ values: true }} component={AutoSave} />
);
