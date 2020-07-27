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
import ReactJson from 'react-json-view';
import { get } from 'lodash';

import { Dialog } from 'components/common/Dialog';

const EventLogContentDialog = (props) => {
  const { data, isOpen, onClose } = props;
  return (
    <>
      <Dialog
        maxWidth="md"
        onClose={onClose}
        open={isOpen}
        showActions={false}
        title={get(data, 'title', '')}
      >
        <ReactJson
          displayDataTypes={false}
          displayObjectSize={false}
          enableClipboard={false}
          iconStyle="square"
          src={get(data, 'payload', {})}
        />
      </Dialog>
    </>
  );
};

EventLogContentDialog.propTypes = {
  data: PropTypes.any,
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func,
};

EventLogContentDialog.defaultProps = {
  onClose: () => {},
};

export default EventLogContentDialog;
