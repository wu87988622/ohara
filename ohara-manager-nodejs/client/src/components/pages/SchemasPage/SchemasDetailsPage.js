import React from 'react';

import AppWrapper from '../../common/AppWrapper';
import Loading from '../../common/Loading';
import { fetchSchemasDetails } from '../../../apis/schemasApis';
import { DataTable } from '../../common/Table';

class SchemasDetailsPage extends React.Component {
  state = {
    details: {},
    columns: [],
    isLoading: true,
  };

  async componentDidMount() {
    const res = await fetchSchemasDetails({
      uuid: this.props.match.params.uuid,
    });

    if (res.status) {
      const { name, disabled, uuid, orders, types } = res.data;

      const columns = Object.keys(orders).map(order => {
        const type = types[order].split('$')[0].toLowerCase();

        return {
          name: order,
          order: orders[order],
          type,
        };
      });

      this.setState({
        details: { name, disabled, uuid },
        columns,
        isLoading: false,
      });
    }
    console.log(res);
  }

  render() {
    const tableHeaders = ['#', 'Column Name', 'Data Type'];
    const { isLoading, columns } = this.state;
    const { name, disabled, uuid } = this.state.details;

    if (isLoading) {
      return <Loading />;
    }

    return (
      <AppWrapper title={'Schema details'}>
        <ul>
          <li>
            <strong>UUID:</strong> {uuid}
          </li>
          <li>
            <strong>Schema name:</strong> {name}
          </li>
          <li>
            <strong>Disabled:</strong> {disabled}
          </li>
        </ul>

        <DataTable headers={tableHeaders} data={columns} />
      </AppWrapper>
    );
  }
}

export default SchemasDetailsPage;
