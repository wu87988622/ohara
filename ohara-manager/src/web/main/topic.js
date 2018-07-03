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
            oharaManager.api.createTopic(createTopicJsonStr, this.onSuccess, this.onFail);
        }
    },
    onSuccess: function(status, uuid, errorMessage) {
        if (status == true) {
           alert("create topic finish. uuid:" + uuid);
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