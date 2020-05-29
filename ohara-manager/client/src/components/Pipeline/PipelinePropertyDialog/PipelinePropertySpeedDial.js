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
import { forIn } from 'lodash';

import SpeedDial from '@material-ui/lab/SpeedDial';
import SpeedDialIcon from '@material-ui/lab/SpeedDialIcon';
import SpeedDialAction from '@material-ui/lab/SpeedDialAction';
import FileCopyIcon from '@material-ui/icons/FileCopyOutlined';
import EditIcon from '@material-ui/icons/Edit';

import { AutofillEditor, AutofillSelector } from 'components/Autofill';
import {
  toAutofillData,
  toFormValues,
} from 'components/Autofill/autofillUtils';

const PipelinePropertySpeedDial = ({ formRef }) => {
  const [isSeepDialOpen, setIsSeepDialOpen] = useState(false);
  const [isAutofillSelectorOpen, setIsAutofillSelectorOpen] = useState(false);
  const [isAutofillEditorOpen, setIsAutofillEditorOpen] = useState(false);
  const [autofillData, setAutofillData] = useState(null);

  const handleClose = () => {
    setIsSeepDialOpen(false);
  };

  const handleOpen = (_, reason) => {
    if (reason === 'mouseEnter' || reason === 'toggle') setIsSeepDialOpen(true);
  };

  const handleCopyButtonClick = () => {
    const formValues = formRef.current.values();
    const definitions = formRef.current.getDefinitions();
    const autofillData = toAutofillData(formValues, definitions);
    setAutofillData(autofillData);
    setIsAutofillEditorOpen(true);
  };

  const handleAutofillButtonClick = () => {
    setIsAutofillSelectorOpen(true);
  };

  const handleAutofill = autofillData => {
    if (formRef.current && autofillData) {
      const definitions = formRef.current.getDefinitions();
      const formValues = toFormValues(autofillData, definitions);
      forIn(formValues, (value, key) => formRef.current.change(key, value));
    }
  };

  return (
    <>
      <SpeedDial
        ariaLabel="Pipeline property speedDial"
        direction="up"
        icon={<SpeedDialIcon />}
        onClose={handleClose}
        onOpen={handleOpen}
        open={isSeepDialOpen}
      >
        <SpeedDialAction
          icon={<EditIcon />}
          key="autofill"
          onClick={handleAutofillButtonClick}
          tooltipTitle="Autofill"
        />
        <SpeedDialAction
          icon={<FileCopyIcon />}
          key="copy"
          onClick={handleCopyButtonClick}
          tooltipTitle="Copy"
        />
      </SpeedDial>
      <AutofillEditor
        data={autofillData}
        isOpen={isAutofillEditorOpen}
        mode="Copy"
        onClose={() => setIsAutofillEditorOpen(false)}
      />
      <AutofillSelector
        isOpen={isAutofillSelectorOpen}
        onClose={() => setIsAutofillSelectorOpen(false)}
        onSubmit={handleAutofill}
      />
    </>
  );
};

PipelinePropertySpeedDial.propTypes = {
  formRef: PropTypes.oneOfType([
    PropTypes.func,
    PropTypes.shape({ current: PropTypes.any }),
  ]),
};

export default PipelinePropertySpeedDial;
