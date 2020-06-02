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
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import Badge from '@material-ui/core/Badge';
import ArrowRightIcon from '@material-ui/icons/ArrowRight';
import IconButton from '@material-ui/core/IconButton';

import { SETTINGS_COMPONENT_TYPES } from 'const';

const SectionList = (props) => {
  const { list, handleChange, sectionHeading, sectionRef } = props;

  return (
    <List className="section-list" component="ul">
      {list.map((item) => {
        const {
          title,
          icon,
          type,
          subTitle,
          badge,
          componentProps = {},
        } = item;
        const { children, handleClick } = componentProps;

        const onClick = (event, title) => {
          switch (type) {
            case SETTINGS_COMPONENT_TYPES.PAGE:
            case SETTINGS_COMPONENT_TYPES.DIALOG:
              return handleChange({
                type,
                name: title,
                ref: sectionRef,
                heading: sectionHeading,
              });

            case SETTINGS_COMPONENT_TYPES.CUSTOMIZED:
              return (
                typeof handleClick === 'function' && handleClick(event, title)
              );

            default:
              throw new Error(`Unknown type of ${type}`);
          }
        };

        return (
          <ListItem key={title} onClick={(event) => onClick(event, title)}>
            <Badge badgeContent={badge?.count} component="div">
              {icon && <ListItemIcon>{icon}</ListItemIcon>}
              <ListItemText primary={title} secondary={subTitle} />

              {type === SETTINGS_COMPONENT_TYPES.PAGE && (
                <ListItemSecondaryAction>
                  <IconButton
                    edge="end"
                    onClick={(event) => onClick(event, title)}
                    size="small"
                  >
                    <ArrowRightIcon fontSize="small" />
                  </IconButton>
                </ListItemSecondaryAction>
              )}

              {type === SETTINGS_COMPONENT_TYPES.CUSTOMIZED && <>{children}</>}
            </Badge>
          </ListItem>
        );
      })}
    </List>
  );
};

SectionList.propTypes = {
  list: PropTypes.arrayOf(
    PropTypes.shape({
      icon: PropTypes.node,
      text: PropTypes.string,
    }),
  ),
  handleChange: PropTypes.func.isRequired,
  selectedComponent: PropTypes.shape({
    name: PropTypes.string,
  }),
  sectionHeading: PropTypes.string.isRequired,
  sectionRef: PropTypes.object,
};

export default React.memo(SectionList);
