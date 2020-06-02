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
import KeyboardBackspaceIcon from '@material-ui/icons/KeyboardBackspace';
import Typography from '@material-ui/core/Typography';
import IconButton from '@material-ui/core/IconButton';
import Box from '@material-ui/core/Box';

import { Dialog } from 'components/common/Dialog';
import { SETTINGS_COMPONENT_TYPES } from 'const';

const SectionComponent = (props) => {
  const { sections, selectedComponent, handleClose } = props;

  if (!selectedComponent) return null;

  return (
    <>
      {sections.map((section) =>
        section.components.map((component) => {
          const { title } = component;
          if (title !== selectedComponent.name) return null;
          return (
            <Box key={title}>
              {componentRenderer({
                component,
                selectedComponent,
                handleClose,
              })}
            </Box>
          );
        }),
      )}
    </>
  );
};

function componentRenderer({ component, selectedComponent, handleClose }) {
  const { title, type, componentProps } = component;

  switch (type) {
    case SETTINGS_COMPONENT_TYPES.PAGE:
      return (
        <div className="section-page">
          <div className="section-page-header">
            <IconButton color="inherit" edge="start" onClick={handleClose}>
              <KeyboardBackspaceIcon />
            </IconButton>
            <Typography component="h2" variant="h4">
              {title}
            </Typography>
          </div>

          <div className="section-page-content">{componentProps?.children}</div>
        </div>
      );

    case SETTINGS_COMPONENT_TYPES.DIALOG:
      return (
        <Dialog
          onClose={handleClose}
          open={title === selectedComponent.name}
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

SectionComponent.propTypes = {
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
  handleClose: PropTypes.func.isRequired,
  selectedComponent: PropTypes.shape({
    name: PropTypes.string,
  }),
};

export default React.memo(SectionComponent);
