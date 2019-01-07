import * as URLS from '../../src/constants/urls';

describe('PipelinePage', () => {
  beforeEach(() => {
    cy.visit(URLS.PIPELINE);
    cy.getByTestId('new-pipeline').click();
  });

  it('creates a pipeline and displays in the pipeline list page', () => {
    const pipelineName = 'Test pipeline';

    cy.getByText('Untitled pipeline')
      .click({ force: true })
      .getByTestId('title-input')
      .clear()
      .type(pipelineName)
      .blur();

    cy.visit(URLS.HOME);
    cy.getByText(pipelineName).should('have.length', 1);

    // TODO: remove testing pipeline
  });

  it('Creates a FTP source connector', () => {
    cy.location('pathname').should('contain', '/pipelines/new/');
    cy.getByTestId('toolbar-sources').click();

    cy.getByText('Add a new source connector').should('have.length', 1);
    cy.getByText('com.island.ohara.connector.ftp.FtpSource').click();

    cy.getByText('Add').click();

    cy.getByText('Untitled Source')
      .should('have.length', '1')
      .get('.node-type')
      .should('contain', 'FtpSource');
  });
});
