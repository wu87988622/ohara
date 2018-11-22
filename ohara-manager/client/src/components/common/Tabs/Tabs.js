import styled from 'styled-components';
import {
  Tab as UnstyledTab,
  TabList as UnstyledTabList,
  Tabs as UnstyledTabs,
  TabPanel as UnstyledTabPanel,
} from 'react-tabs';

import { white, shadowNormal, radiusNormal } from '../../../theme/variables';

const Tabs = styled(UnstyledTabs)`
  background-color: ${white};
  box-shadow: ${shadowNormal};
  border-radius: ${radiusNormal};
  margin-bottom: 20px;
`;

const TabList = styled(UnstyledTabList)`
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  padding: 0;
  margin: 0;
`;

const Tab = styled(UnstyledTab).attrs({
  selectedClassName: 'selected',
  disabledClassName: 'disabled',
})`
  flex-grow: 1;
  text-align: center;
  padding: 12px 0;
  list-style: none;
  cursor: pointer;
  color: #888;
  border-left: 1px solid #e0e0e0;
  border-bottom: 1px solid #e0e0e0;
  font-size: 15px;

  &:first-child {
    border-left: none;
  }

  &.selected {
    color: #0097ff;
    border-bottom: none;
  }

  &.disabled {
    color: #e0e0e0;
    cursor: not-allowed;
  }
`;

const TabPanel = styled(UnstyledTabPanel).attrs({
  selectedClassName: 'selected',
})`
  display: none;
  padding: 20px 25px;
  overflow-x: auto;

  &.selected {
    display: block;
  }
`;

Tab.tabsRole = 'Tab';
Tabs.tabsRole = 'Tabs';
TabPanel.tabsRole = 'TabPanel';
TabList.tabsRole = 'TabList';

export { Tab, TabList, Tabs, TabPanel };
