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
