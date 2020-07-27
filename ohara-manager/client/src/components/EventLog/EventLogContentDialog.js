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
import ReactJson from 'react-json-view';
import { get } from 'lodash';

import { useEventLogContentDialog } from 'context';
import { Dialog } from 'components/common/Dialog';

const EventLogContentDialog = () => {
  const { isOpen, close, data: log } = useEventLogContentDialog();
  return (
    <>
      <Dialog
        maxWidth="md"
        onClose={close}
        open={isOpen}
        showActions={false}
        title={get(log, 'title', '')}
      >
        <ReactJson
          displayDataTypes={false}
          displayObjectSize={false}
          enableClipboard={false}
          iconStyle="square"
          src={get(log, 'payload', {})}
        />
      </Dialog>
    </>
  );
};

export default EventLogContentDialog;
