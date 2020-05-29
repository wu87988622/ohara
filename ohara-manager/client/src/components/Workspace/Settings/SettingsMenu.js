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
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import ListSubheader from '@material-ui/core/ListSubheader';

const SettingsMenu = ({
  menu,
  handleClick,
  selected,
  isPageComponent,
  closePageComponent,
}) => {
  return (
    <div className="settings-menu">
      {menu.map((section, index) => {
        const { subHeader, items } = section;

        return (
          <List
            component="ul"
            key={index}
            subheader={
              <ListSubheader component="div" disableSticky>
                {subHeader}
              </ListSubheader>
            }
          >
            {items.map(({ icon, text, ref }) => {
              const onClick = (event, text) => {
                handleClick({ text, ref });

                if (isPageComponent) {
                  closePageComponent();
                  return;
                }

                ref.current.scrollIntoView();
              };

              return (
                <ListItem
                  button
                  key={text}
                  onClick={event => onClick(event, text)}
                  selected={selected === text}
                >
                  <ListItemIcon>{icon}</ListItemIcon>
                  <ListItemText primary={text} />
                </ListItem>
              );
            })}
          </List>
        );
      })}
    </div>
  );
};

SettingsMenu.propTypes = {
  menu: PropTypes.arrayOf(
    PropTypes.shape({
      subHeader: PropTypes.string,
      items: PropTypes.arrayOf(
        PropTypes.shape({
          icon: PropTypes.node.isRequired,
          text: PropTypes.string.isRequired,
          ref: PropTypes.object,
        }),
      ).isRequired,
    }),
  ).isRequired,
  handleClick: PropTypes.func.isRequired,
  selected: PropTypes.string.isRequired,
  isPageComponent: PropTypes.bool.isRequired,
  closePageComponent: PropTypes.func.isRequired,
};

export default React.memo(SettingsMenu);
