import React from 'react';
import toastr from 'toastr';

import AppWrapper from '../../common/AppWrapper';
import Loading from '../../common/Loading';
import { ListTable } from '../../common/Table';
import Modal from './Modal';
import { fetchSchemas, createSchemas } from '../../../apis/schemasApis';
import { SCHEMAS } from '../../../constants/url';
import * as MESSAGE from '../../../constants/message';

class SchemasPage extends React.Component {
  state = {
    tableHeaders: ['Schema Name', 'Details'],
    schemas: [],
    schemaName: '',
    columnName: '',
    // TODO: these dataTypes should be coming from the API server instead of hard coding in the client side
    dataTypes: [
      'String',
      'Bytes',
      'Boolean',
      'Byte',
      'Short',
      'Int',
      'Long',
      'Float',
      'Double',
    ],
    currDataType: '',
    isDisable: false,
    schemasColumns: [],
    isLoading: true,
    modalIsActive: false,
  };

  componentDidMount() {
    this.setDefaultDataType();
    this.fetchData();
  }

  fetchData = async () => {
    this.setState({ isLoading: true });
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
  };

  handleChangeInput = ({ target: { name, value } }) => {
    this.setState({ [name]: value });
  };

  handleChangeSelect = ({ target: { value: currDataType } }) => {
    this.setState({ currDataType });
  };

  handleAddColumn = () => {
    const { schemasColumns, columnName, currDataType } = this.state;
    const isEmpty = !columnName;
    const isDuplicated = schemasColumns.find(col => col.name === columnName);

    if (isEmpty) {
      toastr.error(MESSAGE.EMPTY_COLUMN_NAME_ERROR);
      return;
    }

    if (isDuplicated) {
      toastr.error(MESSAGE.DUPLICATED_COLUMN_NAME_ERROR);
      return;
    }

    this.setState({
      schemasColumns: [
        ...schemasColumns,
        { name: columnName, type: currDataType },
      ],
    });

    this.resetColumns();
  };

  handleDeleteColumn = name => {
    const schemasColumns = this.state.schemasColumns.filter(
      col => col.name !== name,
    );

    this.setState({ schemasColumns });
  };

  handleSave = async () => {
    const {
      schemaName: name,
      schemasColumns,
      isDisable: disabled,
    } = this.state;

    if (!name) {
      toastr.error(MESSAGE.EMPTY_SCHEMA_NAME_ERROR);
      return;
    }

    if (schemasColumns.length === 0) {
      toastr.error(MESSAGE.EMPTY_SCHEMAS_COLUMNS_ERROR);
      return;
    }

    let types = {};
    const orders = schemasColumns.reduce((acc, { name, type }, idx) => {
      types[name] = `${type.toUpperCase()}$`;
      return { ...acc, [name]: ++idx };
    }, {});

    const params = {
      name,
      disabled,
      types,
      orders,
    };

    const res = await createSchemas(params);

    if (res.status) {
      toastr.success(MESSAGE.SCHEMA_CREATION_SUCCESS);
      this.handleCloseModal();
      this.fetchData();
    }
  };

  handleOpenModal = () => {
    this.setState({ modalIsActive: true });
  };

  handleCloseModal = () => {
    this.setState({ modalIsActive: false });
    this.reset();
  };

  setDefaultDataType = () => {
    this.setState({ currDataType: this.state.dataTypes[0] });
  };

  resetColumns = () => {
    this.setState({ columnName: '' });
    this.setDefaultDataType();
  };

  reset = () => {
    this.setState({ schemaName: '', isDisable: false, schemasColumns: [] });
    this.resetColumns();
  };

  render() {
    const {
      isLoading,
      tableHeaders,
      schemas,
      modalIsActive,
      schemaName,
      columnName,
      dataTypes,
      currDataType,
      schemasColumns,
    } = this.state;

    if (isLoading) {
      return <Loading />;
    }

    return (
      <AppWrapper title="Schemas">
        <button
          onClick={this.handleOpenModal}
          type="button"
          className="btn btn-outline-primary mb-3"
        >
          Create Schemas
        </button>
        <Modal
          isActive={modalIsActive}
          schemaName={schemaName}
          columnName={columnName}
          dataTypes={dataTypes}
          currDataType={currDataType}
          schemasColumns={schemasColumns}
          handleChangeSelect={this.handleChangeSelect}
          handleCloseModal={this.handleCloseModal}
          handleChangeInput={this.handleChangeInput}
          handleAddColumn={this.handleAddColumn}
          handleDeleteColumn={this.handleDeleteColumn}
          handleSave={this.handleSave}
        />
        <ListTable headers={tableHeaders} list={schemas} urlDir={SCHEMAS} />
      </AppWrapper>
    );
  }
}

export default SchemasPage;
