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
import Chip from '@material-ui/core/Chip';
import TextField from '@material-ui/core/TextField';
import Autocomplete from '@material-ui/lab/Autocomplete';

const ArrayDef = () => {
  return (
    <Autocomplete
      multiple
      freeSolo
      renderTags={(value, getTagProps) =>
        value.map((option, index) => (
          <Chip
            variant="outlined"
            size="small"
            label={option}
            {...getTagProps({ index })}
          />
        ))
      }
      renderInput={params => (
        <TextField
          {...params}
          variant="filled"
          label="freeSolo"
          placeholder="Favorites"
          margin="normal"
          fullWidth
        />
      )}
    />
  );
};

export default ArrayDef;
