oharaManager.widget.menu = {
    clickBinding: ["#jobs~clickJobs", "#account~clickAccount", "#monitor~clickMonitor",
                   "#dishboard~clickDishBoard", "#topic~clickTopic", "#schema~clickSchema"],
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
    clickDishBoard: function(e){
      this.contentPanel.clean();
      this.contentHeaderPanel.clean();
      this.contentHeaderPanel.append("Dishboard");
    },
    clickTopic: function(e){
      this.contentPanel.clean();
      this.contentHeaderPanel.clean();
      this.contentHeaderPanel.append("Topic");
    },
    clickSchema: function(e){
      this.contentPanel.clean();
      this.contentHeaderPanel.clean();
      this.contentHeaderPanel.append("Schema");

      var schemaContentPanel = this._template("#template .schemaContentPanel");
      this.contentPanel.append(schemaContentPanel);

      var createSchemaDialogWidget = this.contentPanel.createWidget("#template .createSchemaDialog", ".container-fluid .createSchemaDialog", oharaManager.widget.schemaCreateDialog);
      createSchemaDialogWidget.bind();
    },
    buildItems: function(){
      var cloneToolTemplate = this._template("#template .tool");
      this.$baseEl.append(cloneToolTemplate({toolName: "Jobs", toolID: "jobs", icon: "fas fa-align-left"}))
                      .append(cloneToolTemplate({toolName: "Account", toolID: "account", icon: "fas fa-user"}))
                      .append(cloneToolTemplate({toolName: "Monitor", toolID: "monitor", icon: "fas fa-desktop"}))
                      .append(cloneToolTemplate({toolName: "DishBoard", toolID: "dashboard", icon: "fas fa-chart-bar"}))
                      .append(cloneToolTemplate({toolName: "Topic", toolID: "topic", icon: "fas fa-codepen"}))
                      .append(cloneToolTemplate({toolName: "Schema", toolID: "schema", icon: "fas fa-copy"}));
    }
};
