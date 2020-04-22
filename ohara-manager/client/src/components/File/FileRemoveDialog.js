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
import { isFunction } from 'lodash';
import { DeleteDialog } from 'components/common/Dialog';

const defaultOptions = {
  content: file => `Are you sure you want to remove the file ${file?.name}?`,
  title: 'Remove file?',
};

function FileRemoveDialog(props) {
  const { file, isOpen, onClose, onConfirm } = props;
  const options = { ...defaultOptions, ...props.options };

  return (
    <DeleteDialog
      confirmText="REMOVE"
      content={
        isFunction(options?.content) ? options.content(file) : options?.content
      }
      handleClose={onClose}
      handleConfirm={() => onConfirm(file)}
      open={isOpen}
      title={isFunction(options?.title) ? options.title(file) : options?.title}
    />
  );
}

FileRemoveDialog.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  file: PropTypes.shape({
    name: PropTypes.string,
  }),
  onClose: PropTypes.func,
  onConfirm: PropTypes.func.isRequired,
  options: PropTypes.shape({
    title: PropTypes.oneOfType([PropTypes.string, PropTypes.func]),
    content: PropTypes.oneOfType([PropTypes.string, PropTypes.func]),
  }),
};

FileRemoveDialog.defaultProps = {
  file: null,
  onClose: () => {},
  options: defaultOptions,
};

export default FileRemoveDialog;
