oharaManager.widget.schemaList = {
    listSchema: function() {
        var rowTemplate = this._template("#template .listSchemaTableBody table tbody");
        //TODO OHARA-205 to implement connect resftul api to get schema info
        for (var i = 1 ; i <= 10 ; i++) {
            this.$baseEl.find("table tbody").append(rowTemplate({schemaName: "schema" + i, isDisable: "false"}));
        }
    }
}

oharaManager.widget.schemaCreateDialog = {
    rows: [],
    rowCount: 0,
    clickAddSchemaButton: function(e) {
        var _this = this;
        var columnName = this.$baseEl.find("input[name='columnName']").val();
        var dataType = this.$baseEl.find("select[name='dataType'] :selected").html();
        var dataTypeValue = this.$baseEl.find("select[name='dataType'] :selected").val();
        if (columnName != '' && !this.isExists(columnName)) {
            _this.rowCount = _this.rowCount + 1;
            _this.rows.push({rowID: this.rowCount, columnName: columnName, dataType: dataTypeValue});

            var rowTemplate = this._template("#template .createSchemaTableBody table tbody");
            var rowTR = rowTemplate({rowID: this.rowCount, columnName: columnName, dataType: dataType});
            this.$baseEl.find("table tbody").append(rowTR);
            this.$baseEl.find("table tbody tr:last-child a").attr("columnName", columnName);
            this.$baseEl.find("input[name='columnName']").val("");
            this.$baseEl.find("table tbody tr:last-child a").bind("click", function() {
                _this.rowCount = _this.rowCount - 1;
                var tr = $(this).parent().parent();
                _this.deleteSchemaRowUI(tr);
                var selectColumnName = tr.find("td:nth-child(2)").html();
                _this.rebuildRowArray(selectColumnName);
            });
        } else {
            alert("The column name empty or duplicate");
        }
    },
    clickSaveSchemaButton: function(e) {
        var _this = this;
        var schemaName = this.$baseEl.find("input[name='schemaName']").val();
        if(schemaName != '' && this.rows.length > 0) {
            var isDisable = $("input[name='isDisable']").prop("checked");
            var createSchemaJsonString = this.buildCreateSchemaJsonString(schemaName, isDisable, this.rows);
            oharaManager.api.createSchema(createSchemaJsonString, this.onSuccess, this.onFail);
        } else {
            alert("The schema name or column is empty. Please input your schema name or add column");
        }
    },
    onSuccess: function(status, uuid, errorMessage) {
        if (status == "true") {
           alert("create schema finish. uuid:" + uuid);
        } else {
           alert("create schema failed:\n" + errorMessage);
        }
    },
    onFail: function(errorMessage) {
       console.log(errorMessage);
    },
    buildCreateSchemaJsonString: function(schemaName, isDisable, rows) {
        var types = this.rows.map(x => '"' + x.columnName + '": "' + x.dataType + '"').join(',');
        var orders = this.rows.map(x => '"' + x.columnName + '": "' + x.rowID + '"').join(',');
        var jsonStr =        '{"name": "' + schemaName + '",';
        jsonStr = jsonStr +  '"types": {' + types + '},';
        jsonStr = jsonStr +  '"orders": {' + orders + '},';
        jsonStr = jsonStr +  '"disabled":"' + isDisable + '"}';
        return jsonStr;
    },
    deleteSchemaRowUI: function($tr) {
        $tr.remove();
        for (var i in this.rows) {
            var count = parseInt(i) + 1;
            this.$baseEl.find("table tbody tr:nth-child(" + count  + ") td:nth-child(1)").empty().append(count);
        }
    },
    rebuildRowArray: function(selectColumnName) {
        for (var i in this.rows) {
            if (this.rows[i].columnName == selectColumnName) {
                this.rows.splice(i, 1);
            }
        }
        for (var i in this.rows) {
            this.rows[i].rowID = parseInt(i) + 1; // rowID from 1 to start
        }
    },
    isExists: function(columnName) {
        if ($.map(this.rows, function(item, index) { return item.columnName }).indexOf(columnName) == -1) {
            return false;
        } else {
            return true;
        }
    },
    bind: function() {
        var _this = this;
        this.$find("#addSchemaButton").bind("click", function() { _this.clickAddSchemaButton() });
        this.$find("#saveSchemaButton").bind("click", function() { _this.clickSaveSchemaButton() });
        this.rows = [];
        this.rowCount = 0;
    }
}
