oharaManager.widget.menu = {
    clickBinding: ["#jobs~clickJobs", "#account~clickAccount", "#monitor~clickMonitor",
                   "#dashboard~clickDashBoard", "#topic~clickTopic", "#schema~clickSchema"],
    clickJobs: function(e){
      this.contentPanel.clean();
      this.contentHeaderPanel.clean();
      this.contentHeaderPanel.append("JOBS");
    },
    clickAccount: function(e){
      this.contentPanel.clean();
      this.contentHeaderPanel.clean();
      this.contentHeaderPanel.append("Account");
    },
    clickMonitor: function(e){
      this.contentPanel.clean();
      this.contentHeaderPanel.clean();
      this.contentHeaderPanel.append("Monitor");
    },
    clickDashBoard: function(e){
      this.contentPanel.clean();
      this.contentHeaderPanel.clean();
      this.contentHeaderPanel.append("Dashboard");
    },
    clickTopic: function(e){
      this.contentPanel.clean();
      this.contentHeaderPanel.clean();
      this.contentHeaderPanel.append("Topic");

      var topicContentPanel = this._template("#template .topicContentPanel");
      this.contentPanel.append(topicContentPanel);

      var createTopicDialogWidget = this.contentPanel.createWidget("#template .createTopicDialog", ".container-fluid .createTopicDialog", oharaManager.widget.topicCreateDialog);
      createTopicDialogWidget.bind();
    },
    clickSchema: function(e){
      this.contentPanel.clean();
      this.contentHeaderPanel.clean();
      this.contentHeaderPanel.append("Schema");

      var schemaContentPanelWidget = this.contentPanel.createWidget("#template .schemaContentPanel", ".container-fluid .listSchemaPanel", oharaManager.widget.schemaList);
      schemaContentPanelWidget.listSchema();

      var createSchemaDialogWidget = this.contentPanel.createWidget("#template .createSchemaDialog", ".container-fluid .createSchemaDialog", oharaManager.widget.schemaCreateDialog);
      var cfg = $.extend({
          "listSchemaPanel": schemaContentPanelWidget
      }, createSchemaDialogWidget);
      cfg.bind();
    },
    buildItems: function(){
      var cloneToolTemplate = this._template("#template .tool");
      this.$baseEl.append(cloneToolTemplate({toolName: "Jobs", toolID: "jobs", icon: "fas fa-align-left"}))
                      .append(cloneToolTemplate({toolName: "Account", toolID: "account", icon: "fas fa-user"}))
                      .append(cloneToolTemplate({toolName: "Monitor", toolID: "monitor", icon: "fas fa-desktop"}))
                      .append(cloneToolTemplate({toolName: "DashBoard", toolID: "dashboard", icon: "fas fa-chart-bar"}))
                      .append(cloneToolTemplate({toolName: "Topic", toolID: "topic", icon: "fas fa-codepen"}))
                      .append(cloneToolTemplate({toolName: "Schema", toolID: "schema", icon: "fas fa-copy"}));
    }
};
