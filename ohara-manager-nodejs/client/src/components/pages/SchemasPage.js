import React from 'react';

import AppWrapper from '../common/AppWrapper';
import Loading from '../common/Loading';
import Table from '../common/Table';
import { fetchSchemas } from '../../apis/schemasApis';
import { SCHEMAS } from '../../constants/url';

class SchemasPage extends React.Component {
  state = {
    headers: ['Schema Name', 'Details'],
    schemas: [],
    isLoading: true,
  };

  async componentDidMount() {
    const res = await fetchSchemas();
    if (res) {
      const { uuids } = res.data;
      const schemas = Object.keys(uuids).map(key => {
        return {
          [key]: uuids[key],
        };
      });

      this.setState({ schemas, isLoading: false });
    }
  }

  render() {
    const { isLoading, headers, schemas } = this.state;

    if (isLoading) {
      return <Loading />;
    }
    return (
      <AppWrapper title="Schemas">
        <Table headers={headers} list={schemas} urlDir={SCHEMAS} />
      </AppWrapper>
    );
  }
}

export default SchemasPage;
