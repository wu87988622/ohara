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
import cx from 'classnames';
import PropTypes from 'prop-types';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import KeyboardBackspaceIcon from '@material-ui/icons/KeyboardBackspace';
import Badge from '@material-ui/core/Badge';
import Typography from '@material-ui/core/Typography';
import IconButton from '@material-ui/core/IconButton';
import Box from '@material-ui/core/Box';
import ArrowRightIcon from '@material-ui/icons/ArrowRight';
import { isEmpty } from 'lodash';

import * as hooks from 'hooks';
import { SETTINGS_COMPONENT_TYPES } from 'const';
import { Wrapper } from './SettingsMainStyles';
import { Dialog } from 'components/common/Dialog';
import RestartIndicator from './RestartIndicator';

const SettingsMain = ({
  sections,
  handleChange,
  selectedComponent,
  handleClose,
}) => {
  const isDialog = selectedComponent?.type === SETTINGS_COMPONENT_TYPES.DIALOG;
  const sectionWrapperCls = cx('section-wrapper', {
    'should-display': isEmpty(selectedComponent) || isDialog,
  });

  const discardWorkspace = hooks.useDiscardWorkspaceChangedSettingsAction();
  const { shouldBeRestartWorkspace } = hooks.useShouldBeRestartWorkspace();

  return (
    <Wrapper>
      <div className={sectionWrapperCls}>
        <RestartIndicator
          isOpen={shouldBeRestartWorkspace}
          onDiscard={discardWorkspace}
        />
        {sections.map(section => {
          const { heading, components, ref } = section;
          const listWrapperCls = cx('list-wrapper', {
            'is-danger-zone': heading === 'Danger Zone',
          });

          return (
            <section key={heading}>
              <div className="anchor-element" ref={ref} />
              <Typography variant="h5" component="h2">
                {heading}
              </Typography>
              <div className={listWrapperCls}>
                <List component="ul">
                  {components.map(component => {
                    const {
                      title,
                      icon,
                      type,
                      subTitle,
                      badge,
                      componentProps = {},
                    } = component;
                    const { children, handleClick } = componentProps;

                    const onClick = (event, title) => {
                      switch (type) {
                        case SETTINGS_COMPONENT_TYPES.PAGE:
                        case SETTINGS_COMPONENT_TYPES.DIALOG:
                          return handleChange({
                            name: title,
                            type,
                            heading,
                            ref,
                          });

                        case SETTINGS_COMPONENT_TYPES.CUSTOMIZED:
                          return (
                            typeof handleClick === 'function' &&
                            handleClick(event, title)
                          );

                        default:
                          throw new Error(`Unknown type of ${type}`);
                      }
                    };

                    return (
                      <ListItem
                        key={title}
                        onClick={event => onClick(event, title)}
                      >
                        <Badge component="div" badgeContent={badge?.count}>
                          {icon && <ListItemIcon>{icon}</ListItemIcon>}
                          <ListItemText primary={title} secondary={subTitle} />

                          {type === SETTINGS_COMPONENT_TYPES.PAGE && (
                            <ListItemSecondaryAction>
                              <IconButton
                                edge="end"
                                size="small"
                                onClick={event => onClick(event, title)}
                              >
                                <ArrowRightIcon fontSize="small" />
                              </IconButton>
                            </ListItemSecondaryAction>
                          )}

                          {type === SETTINGS_COMPONENT_TYPES.CUSTOMIZED && (
                            <>{children}</>
                          )}
                        </Badge>
                      </ListItem>
                    );
                  })}
                </List>
              </div>
            </section>
          );
        })}
      </div>

      {selectedComponent &&
        sections.map(section =>
          section.components.map(component => {
            const { title } = component;
            return (
              <Box hidden={title !== selectedComponent.name} key={title}>
                {componentRenderer({
                  component,
                  selectedComponent,
                  handleClose,
                })}
              </Box>
            );
          }),
        )}
    </Wrapper>
  );
};

function componentRenderer({ component, selectedComponent, handleClose }) {
  const { title, type, componentProps } = component;

  switch (type) {
    case SETTINGS_COMPONENT_TYPES.PAGE:
      return (
        <div className="section-page">
          <div className="section-page-header">
            <IconButton edge="start" color="inherit" onClick={handleClose}>
              <KeyboardBackspaceIcon />
            </IconButton>
            <Typography variant="h4" component="h2">
              {title}
            </Typography>
          </div>

          <div className="section-page-content">{componentProps?.children}</div>
        </div>
      );

    case SETTINGS_COMPONENT_TYPES.DIALOG:
      return (
        <Dialog
          open={title === selectedComponent.name}
          onClose={handleClose}
          {...componentProps}
        />
      );

    case SETTINGS_COMPONENT_TYPES.CUSTOMIZED:
      return null;

    default:
      throw new Error(
        `Couldn't render the component with an unknown type of ${type}`,
      );
  }
}

SettingsMain.propTypes = {
  sections: PropTypes.arrayOf(
    PropTypes.shape({
      heading: PropTypes.string.isRequired,
      ref: PropTypes.object,
      components: PropTypes.arrayOf(
        PropTypes.shape({
          icon: PropTypes.node,
          text: PropTypes.string,
        }),
      ),
    }),
  ).isRequired,
  handleChange: PropTypes.func.isRequired,
  handleClose: PropTypes.func.isRequired,
  selectedComponent: PropTypes.shape({
    name: PropTypes.string,
    type: PropTypes.string,
  }),
};

export default React.memo(SettingsMain);
