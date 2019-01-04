import React from 'react';
import { shallow } from 'enzyme';

import DataTable from '../DataTable';

const props = {
  headers: ['#', 'name', 'type'],
};

const data = [['1', '2', '3'], ['4', '5', '6']];

const Children = () => (
  <React.Fragment>
    {data.map((d, idx) => (
      <tr key={idx}>
        {d.map(x => (
          <td key={idx * 4}>{x}</td>
        ))}
      </tr>
    ))}
  </React.Fragment>
);

describe.only('<DataTable />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<DataTable {...props} />);
  });

  it('renders nothing when props.children is not supplied', () => {
    expect(wrapper.text()).toBe('');
    expect(wrapper.name()).not.toBe('Table');
  });

  it('renders the table when props.children is supplied', () => {
    wrapper.setProps({ children: Children() });

    expect(wrapper.length).toBe(1);
    expect(wrapper.name()).toBe('Table');
  });

  it('renders the correct props.table', () => {
    wrapper.setProps({ children: Children() });

    const ths = wrapper.find('tr Th');
    expect(ths.length).toBe(props.headers.length);

    ths.forEach((t, idx) => {
      expect(t.children().text()).toBe(props.headers[idx]);
    });

    const trs = wrapper.find('tbody > tr');
    expect(trs.length).toBe(data.length);

    const firstRow = trs.at(0).find('td');
    const secondRow = trs.at(1).find('td');

    expect(firstRow.length).toBe(data[0].length);

    firstRow.forEach((td, idx) => {
      expect(td.text()).toBe(data[0][idx]);
    });

    expect(secondRow.length).toBe(data[1].length);

    secondRow.forEach((td, idx) => {
      expect(td.text()).toBe(data[1][idx]);
    });
  });
});
