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

import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { isFunction, noop } from 'lodash';
import { DeleteDialog } from 'components/common/Dialog';

const defaultOptions = {
  content: (topic) =>
    `Are you sure you want to delete the topic ${topic?.name} ? This action cannot be undone!`,
  title: 'Delete topic?',
};

function TopicDeleteDialog({
  topic,
  isOpen,
  onClose,
  onConfirm,
  ...restProps
}) {
  const options = { ...defaultOptions, ...restProps.options };
  const [loading, setLoading] = useState(false);

  const handleConfirm = () => {
    setLoading(true);
    const res = onConfirm(topic);
    if (res instanceof Promise) {
      res
        .then(() => {
          setLoading(false);
          onClose();
        })
        .catch(() => {
          setLoading(false);
          onClose();
        });
    } else {
      setLoading(false);
      onClose();
    }
  };

  return (
    <DeleteDialog
      content={
        isFunction(options?.content) ? options.content(topic) : options?.content
      }
      isWorking={loading}
      onClose={loading ? noop : onClose}
      onConfirm={handleConfirm}
      open={isOpen}
      title={isFunction(options?.title) ? options.title(topic) : options?.title}
    />
  );
}

TopicDeleteDialog.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  topic: PropTypes.shape({
    name: PropTypes.string,
  }),
  onClose: PropTypes.func,
  onConfirm: PropTypes.func.isRequired,
  options: PropTypes.shape({
    title: PropTypes.oneOfType([PropTypes.string, PropTypes.func]),
    content: PropTypes.oneOfType([PropTypes.string, PropTypes.func]),
  }),
};

TopicDeleteDialog.defaultProps = {
  topic: null,
  onClose: () => {},
  options: defaultOptions,
};

export default TopicDeleteDialog;
