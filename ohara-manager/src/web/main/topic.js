oharaManager.widget.topicListPanel = {
   listTopic: function() {
       this.$baseEl.find("table tbody").empty();
       oharaManager.api.listTopics(this, this.onSuccess, this.onFail);
   },
   onSuccess: function(_this, status, uuids) {
        if (status == true) {
          var rowTemplate = _this._template("#template .listTopicTableBody table tbody");
          for(var uuid in uuids) {
              var topicName = uuids[uuid];
              var row = rowTemplate({topicName: topicName});
              _this.$find("table tbody").append(row);
          }
        } else {
            //TODO exception message
            alert("List topic error");
        }
   },
   onFail: function(errorMessage) {
        console.log(errorMessage);
   }
}

oharaManager.widget.topicCreateDialog = {
    clickSaveTopicButton: function() {
        var topicName = this.$baseEl.find("input[name='topicName']").val();
        var partition = this.$baseEl.find("input[name='partition']").val();
        var replication = this.$baseEl.find("input[name='replication']").val();
        if (partition == '' || replication == '') {
            alert("Please input topic name, partition, and replicatioin text value");
        } else if (isNaN(partition) || isNaN(replication)) {
            alert("The partition and replication, please input the Number");
        } else {
            var createTopicJsonStr = this.buildCreateTopicJsonString(topicName, partition, replication);
            oharaManager.api.createTopic(this, createTopicJsonStr, this.onSuccess, this.onFail);

        }
    },
    onSuccess: function(_this, status, uuid, errorMessage) {
        if (status == true) {
           alert("create topic finish. uuid:" + uuid);
           _this.listTopicPanel.listTopic();
        } else {
           alert("create topic failed:\n" + errorMessage);
        }
    },
    onFail: function(errorMessage) {
        console.log(errorMessage);
    },
    buildCreateTopicJsonString: function(topicName, partition, replication) {
        var jsonStr =       '{"name": "' + topicName + '",';
        jsonStr = jsonStr +  '"numberOfPartitions": ' + partition + ',';
        jsonStr = jsonStr +  '"numberOfReplications": ' + replication + '}';
        return jsonStr;
    },
    bind: function() {
        var _this = this;
        this.$find("#saveTopicButton").bind("click", function() { _this.clickSaveTopicButton(); });
    }
};