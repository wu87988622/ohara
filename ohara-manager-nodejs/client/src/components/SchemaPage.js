import React from 'react';
import styled from 'styled-components';

import Header from './common/Header';
import AppWrapper from './common/AppWrapper';

const SchemaWrapper = styled.div`
  cursor: pointer;
  display: inline-block;
  border: 10px solid lightslategray;
  padding: 20px;
  transition: all 0.5s;
  border-radius: 4px;

  &.isActive {
    transition: all 0.5s;
    border-color: #ff4444;
  }
`;

class SchemaPage extends React.Component {
  // Component states
  state = {
    isLoaded: false,
  };

  // Life cycle methods
  componentDidMount() {
    // When this component is mounted, do the following:
    // console.log('Mounting the component');

    // Update the state isLoaded to true
    this.setState({ isLoaded: true });
  }

  componentWillUpdate() {
    // console.log('Will update the page');
  }

  componentWillUnmount() {
    // console.log('unmounting');
  }

  // Event handler
  handleClick = () => {
    // Toggle the state isLoaded
    this.setState(({ isLoaded }) => {
      return { isLoaded: !isLoaded };
    });
  };

  render() {
    // Get the className string based on state isLoaded
    const isActiveCls = this.state.isLoaded ? 'isActive' : '';

    return (
      <AppWrapper>
        <Header />
        <SchemaWrapper
          className={`SchemaPage ${isActiveCls}`}
          onClick={this.handleClick}
        >
          I'm Schema page, App is ready ? {this.state.isLoaded ? 'Yes' : 'Nope'}
        </SchemaWrapper>
      </AppWrapper>
    );
  }
}

export default SchemaPage;
