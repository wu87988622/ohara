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
import { Tab, Tabs, TabList, TabPanel } from 'common/Tabs';

class UtilsTabs extends React.Component {
  static propTypes = {
    groupDefs: PropTypes.array.isRequired,
    defsToFormGroup: PropTypes.func.isRequired,
  };
  state = {
    tabIndex: 0,
  };
  render() {
    return (
      <Tabs
        selectedIndex={this.state.tabIndex}
        onSelect={tabIndex => this.setState({ tabIndex })}
      >
        <TabList>
          {this.props.groupDefs.sort().map(defs => {
            return <Tab key={defs[0].group}>{defs[0].group}</Tab>;
          })}
        </TabList>
        {this.props.groupDefs.sort().map(defs => {
          return (
            <TabPanel key={defs[0].group}>
              {this.props.defsToFormGroup(defs)}
            </TabPanel>
          );
        })}
      </Tabs>
    );
  }
}
export default UtilsTabs;
