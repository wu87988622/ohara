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
import styled from 'styled-components';
import AppBar from '@material-ui/core/AppBar';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';

const StyledAppBar = styled(AppBar)`
  margin-bottom: 20px;
`;

const MuiTab = ({
  topics,
  handleChange,
  handleColumnChange,
  handleColumnRowDelete,
  handleColumnRowUp,
  handleColumnRowDown,
  groupedDefs,
  renderer,
}) => {
  const [activeIdx, setActiveIdx] = React.useState(0);

  const handleIdxChange = (e, idx) => {
    setActiveIdx(idx);
  };

  const formProps = {
    topics,
    handleChange,
    handleColumnChange,
    handleColumnRowDelete,
    handleColumnRowUp,
    handleColumnRowDown,
  };

  return (
    <>
      <StyledAppBar position="static">
        <Tabs value={activeIdx} onChange={handleIdxChange}>
          {groupedDefs.sort().map(defs => {
            return <Tab key={defs[0].group} label={defs[0].group} />;
          })}
        </Tabs>
      </StyledAppBar>

      {groupedDefs.sort().map((defs, idx) => {
        if (idx !== activeIdx) return null; // We just want to render the active tab content

        const renderData = { ...formProps, formData: defs };
        return <div key={defs[0].group}>{renderer(renderData)}</div>;
      })}
    </>
  );
};

MuiTab.propTypes = {
  groupedDefs: PropTypes.array.isRequired,
  renderer: PropTypes.func.isRequired,
  topics: PropTypes.array,
  handleChange: PropTypes.func,
  handleColumnChange: PropTypes.func,
  handleColumnRowDelete: PropTypes.func,
  handleColumnRowUp: PropTypes.func,
  handleColumnRowDown: PropTypes.func,
};

export default MuiTab;
