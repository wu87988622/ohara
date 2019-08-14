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
import { cleanup, fireEvent, render } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';

import * as generate from 'utils/generate';
import SortTable from '../SortTable';

const setup = (override = {}) => {
  return {
    headRows: [{ id: 'header', label: 'header' }],
    rows: [{ header: 'value' }],
    isLoading: false,
    ...override,
  };
};

describe('<SortTable />', () => {
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('renders sortTable', async () => {
    render(<SortTable {...setup()} />);
  });

  it('renders loader', async () => {
    const props = setup({ isLoading: true });
    const { getByTestId } = render(<SortTable {...props} />);

    getByTestId('table-loader');
  });

  it('renders sortTable header', async () => {
    const props = setup({
      headRows: [
        {
          id: generate.name(),
          label: generate.name(),
        },
        {
          id: generate.name(),
          label: generate.name(),
        },
      ],
    });
    const { getByText } = render(<SortTable {...props} />);
    getByText(props.headRows[0].label);
    getByText(props.headRows[1].label);
  });

  it('displays sortTable row values', async () => {
    const props = setup({
      rows: [{ header: 'test1' }, { header: 'test2' }],
    });
    const { getByText } = render(<SortTable {...props} />);

    getByText(props.rows[0].header);
    getByText(props.rows[1].header);
  });

  it('renders sortTable header without sort button', async () => {
    const props = setup({
      headRows: [
        {
          id: generate.name(),
          label: generate.name(),
          sortable: false,
        },
      ],
      buttonTestId: generate.name(),
    });

    const { queryByTestId } = render(<SortTable {...props} />);

    expect(queryByTestId(props.buttonTestId)).toBeNull();
  });

  it('handles sort button click', async () => {
    const props = setup({
      headRows: [{ id: 'sortHeader', label: 'sortHeader' }],
      tableName: generate.name(),
      dataRowTestId: generate.name(),
      buttonTestId: generate.name(),
      rows: [
        { sortHeader: 'test1' },
        { sortHeader: 'test2' },
        { sortHeader: 'test3' },
      ],
    });

    const { getByTestId, getAllByTestId } = render(<SortTable {...props} />);

    const dataRows = getAllByTestId(props.dataRowTestId);
    //data default sort by asc
    expect(dataRows[0].cells[0].textContent).toBe(props.rows[0].sortHeader);
    expect(dataRows[1].cells[0].textContent).toBe(props.rows[1].sortHeader);
    expect(dataRows[2].cells[0].textContent).toBe(props.rows[2].sortHeader);

    fireEvent.click(getByTestId(props.buttonTestId));
    const dataRowsDesc = getAllByTestId(props.dataRowTestId);
    //data sort by desc
    expect(dataRowsDesc[0].cells[0].textContent).toBe(props.rows[2].sortHeader);
    expect(dataRowsDesc[1].cells[0].textContent).toBe(props.rows[1].sortHeader);
    expect(dataRowsDesc[2].cells[0].textContent).toBe(props.rows[0].sortHeader);

    fireEvent.click(getByTestId(props.buttonTestId));
    const dataRowsAsc = getAllByTestId(props.dataRowTestId);
    //data sort by asc
    expect(dataRowsAsc[0].cells[0].textContent).toBe(props.rows[0].sortHeader);
  });
});
