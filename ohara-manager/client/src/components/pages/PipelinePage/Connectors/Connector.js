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
import { Form } from 'react-final-form';

import * as utils from './connectorUtils';
import * as types from 'propTypes/pipeline';
import Controller from './Controller';
import TestConfigBtn from './TestConfigBtn';
import AutoSave from './AutoSave';
import { TitleWrapper, H5Wrapper } from './styles';
import { Box } from 'components/common/Layout';
import { CONNECTOR_ACTIONS } from 'constants/pipelines';

const Connector = props => {
  const { updateHasChanges, connectors } = props;
  const [state, setState, configs, setConfigs] = utils.useFetchConnectors(
    props,
  );
  const [isTestingConfig, handleTestConfig] = utils.useTestConfig(props);
  const topics = utils.useTopics(props);

  const { connectorName } = props.match.params;

  if (!configs) return null;

  const [targetConnector] = props.graph.filter(g => g.name === connectorName);
  const configFormTitle = targetConnector.className.split('.').pop();

  const [defs] = connectors.filter(
    connector => connector.className === targetConnector.className,
  );

  const formData = utils.getRenderData({
    defs: defs.definitions,
    configs,
    state,
  });

  const initialValues = formData.reduce((acc, cur) => {
    acc[cur.key] = cur.displayValue;
    return acc;
  }, {});

  const columnHandlerParams = {
    configs,
    updateHasChanges,
    setConfigs,
  };

  const formProps = {
    formData,
    topics,
    handleColumnChange: utils.handleColumnChange(columnHandlerParams),
    handleColumnRowDelete: utils.handleColumnRowDelete(columnHandlerParams),
    handleColumnRowUp: utils.handleColumnRowUp(columnHandlerParams),
    handleColumnRowDown: utils.handleColumnRowDown(columnHandlerParams),
  };

  const controllerParams = {
    props,
    setState,
  };

  return (
    <Box>
      <TitleWrapper>
        <H5Wrapper>{configFormTitle}</H5Wrapper>
        <Controller
          kind="connector"
          connectorName={connectorName}
          onStart={() =>
            utils.handleStartConnector({
              action: CONNECTOR_ACTIONS.start,
              ...controllerParams,
            })
          }
          onStop={() =>
            utils.handleStopConnector({
              action: CONNECTOR_ACTIONS.stop,
              ...controllerParams,
            })
          }
          onDelete={() => utils.handleDeleteConnector(state, props)}
        />
      </TitleWrapper>
      <Form
        onSubmit={() => {}}
        initialValues={initialValues}
        render={({ values }) => {
          return (
            <>
              <AutoSave
                save={() => utils.useSave(props, values)}
                updateHasChanges={updateHasChanges}
              />
              {utils.renderForm({ parentValues: values, ...formProps })}
              <TestConfigBtn
                handleClick={event => handleTestConfig(event, values)}
                isWorking={isTestingConfig}
              />
            </>
          );
        }}
      />
    </Box>
  );
};

Connector.propTypes = types.connector;

export default Connector;
