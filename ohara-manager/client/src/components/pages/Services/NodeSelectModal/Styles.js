import styled from 'styled-components';

import { DataTable } from 'common/Table';
import { Input } from 'common/Form';
import * as CSS_VARS from 'theme/variables';

const Table = styled(DataTable)`
  text-align: left;

  .is-running {
    background: ${CSS_VARS.trBgColor};
  }
`;

const Checkbox = styled(Input).attrs({
  type: 'checkbox',
})`
  width: 1rem;
`;

export { Table, Checkbox };
