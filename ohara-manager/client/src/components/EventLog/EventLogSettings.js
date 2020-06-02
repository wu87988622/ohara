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

import { Form, Field } from 'react-final-form';

import Button from '@material-ui/core/Button';
import Grid from '@material-ui/core/Grid';
import Paper from '@material-ui/core/Paper';
import Typography from '@material-ui/core/Typography';

import { InputField, Checkbox } from 'components/common/Form';
import { minNumber, composeValidators } from 'utils/validate';
import Wrapper from './EventLogSettingsStyles';
import * as hooks from 'hooks';

const EventLogSettings = (props) => {
  const updateSettings = hooks.useUpdateEventSettingsAction();
  const { data: eventSettings } = hooks.useEventSettings();

  const onSubmit = (values) => {
    updateSettings(values);
    props.onSave();
  };

  return (
    <Form
      initialValues={eventSettings}
      onSubmit={onSubmit}
      render={({ handleSubmit, submitting, pristine, invalid, values }) => {
        return (
          <Wrapper>
            <Paper>
              <Grid container direction="column" spacing={2}>
                <Grid item>
                  <Typography variant="h6">Event Logs Settings</Typography>
                </Grid>
                <Grid item>
                  <Field
                    component={InputField}
                    disabled={values.unlimited}
                    label="Scrollback logs"
                    name="limit"
                    placeholder="The maximum amount of logs."
                    type="number"
                    validate={composeValidators(minNumber(1))}
                  />
                </Grid>
                <Grid item>
                  <Field
                    component={Checkbox}
                    label="Unlimited logs"
                    name="unlimited"
                    type="checkbox"
                  />
                </Grid>
                <Grid className="align-right" item>
                  <Button
                    color="primary"
                    disabled={submitting || pristine || invalid}
                    onClick={handleSubmit}
                    variant="contained"
                  >
                    Save
                  </Button>
                </Grid>
              </Grid>
            </Paper>
          </Wrapper>
        );
      }}
    />
  );
};

EventLogSettings.propTypes = {
  onSave: PropTypes.func,
};

export default EventLogSettings;
