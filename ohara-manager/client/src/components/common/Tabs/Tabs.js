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

import styled from 'styled-components';
import {
  Tab as UnstyledTab,
  TabList as UnstyledTabList,
  Tabs as UnstyledTabs,
  TabPanel as UnstyledTabPanel,
} from 'react-tabs';

const Tabs = styled(UnstyledTabs)`
  background-color: ${props => props.theme.white};
  box-shadow: ${props => props.theme.shadowNormal};
  border-radius: ${props => props.theme.radiusNormal};
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
  font-size: 15px;
  flex-grow: 1;
  text-align: center;
  padding: 12px 0;
  list-style: none;
  cursor: pointer;
  color: ${props => props.theme.lightBlue};
  border-bottom: 1px solid ${props => props.theme.lighterGray};
  transition: ${props => props.theme.durationNormal} all;

  &:hover {
    transition: ${props => props.theme.durationNormal} color;
    color: ${props => props.theme.blue};
  }

  &:first-child {
    border-left: none;
  }

  &.selected {
    color: ${props => props.theme.blue};
    border-bottom: 2px solid ${props => props.theme.blue};
  }

  &.disabled {
    color: ${props => props.theme.lighterGray};
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
