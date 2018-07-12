import React from 'react';

import Header from './common/Header';
import AppWrapper from './common/AppWrapper';

const TopicPage = () => {
  return (
    <AppWrapper>
      <Header />
      <p data-test="content">See the topics here!</p>
    </AppWrapper>
  );
};

export default TopicPage;
