
(function( $ ) {

    // TODO: Custom Named Events
    // TODO: Add constructor
    /**
     * return object:
     * @param prototype
     * return
     *   attribute
     *     $baseEl
     *     clickBinding
     *   method
     *     $val(id, val)
     *     $find(selector)
     */
    $.fn.widget = function(prototype) {
        // var settings = $.extend({
        // }, options );

        var base = function(){
            var _this = this;
            var $baseEl = $(this);
            var _fieldVal = function(elId, val){
                var $field = $baseEl.find(elId);
                if (!val) return $field.val();
                else return $field.val(val);
            };
            var _bindClickEvent = function(idx, bindString){
                var items = bindString.split("~");
                var $target = $baseEl.find(items[0]);
                var handler = items[1];
                if (result[handler] && $target.length>0) {
                    $target.click( function(){
                        result[handler](this);
                    });
                }
            };

            var result = $.extend({
                $baseEl: $baseEl,
                autoBinding: true,
                bindClickEvent: function(){
                  $.each(this.clickBinding, _bindClickEvent);
                },
                _template: function(selector){
                    _.templateSettings = {
                        interpolate: /\{\{(.+?)\}\}/g
                    };
                    return _.template($(selector).clone().html());
                },
                createWidget: function(selectorTemplate, selector, prototype) {
                    // create dom from template
                     var template = this._template(selectorTemplate);
                    // append into baseEl
                     this.$baseEl.append(template);
                    // $().widget(prototype) and return
                     return $(selector).widget(prototype);
                }
            }, prototype);

            result.$val = function(id, val){
                var $field = result.$find(id);
                if ($field.length>0) {
                    if (!val) return $field.val();
                    else return $field.val(val);
                } else {
                    throw "Cannot find element '" + id + "'";
                }
            };

            result.$find = function(selector){
                return $baseEl.find(selector);
            };

            if (result.autoBinding && result.clickBinding) {
                result.bindClickEvent().call(result);
            }
            return result;
        };
        return base.call(this);
    };

}( jQuery ));

var oharaManager = {
    widget: {}
};

oharaManager.api = {
    createTopic: function(createTopicJSONStr, successCallback, errorCallback) {
        $.post({
          url: '../api/topics',
          contentType: 'application/json',
          dataType: 'json',
          data: createTopicJSONStr,
          success: function(result) {
             var isCreateTopicSuccess = result.status;
             var uuid = result.uuid;
             var errorMessage = result.message;
             successCallback(isCreateTopicSuccess, uuid, errorMessage);
          },
          error: function(XMLHttpRequest, textStatus, errorThrown) {
              var errorMessage = XMLHttpRequest.statusText;
              errorCallback(errorMessage);
          }
        })
    },
    createSchema: function(_this, createSchemaJSONStr, successCallback, errorCallback) {
        $.post({
          url: '../api/schemas',
          contentType: 'application/json',
          dataType: 'json',
          data: createSchemaJSONStr,
          success: function(result) {
             var isCreateSchemaSuccess = result.status;
             var uuid = result.uuid;
             var errorMessage = result.description;
             successCallback(_this, isCreateSchemaSuccess, uuid, errorMessage);
          },
          error: function(XMLHttpRequest, textStatus, errorThrown) {
             var errorMessage = XMLHttpRequest.statusText;
             errorCallback(errorMessage);
          }
        })
    },
    listSchemas: function(_this, successCallback, errorCallback) {
        $.get({
          url: '../api/schemas',
          dataType: 'json',
          success: function(result) {
             var status = result.status;
             var uuids = result.uuids;
             successCallback(_this, status, uuids);
          },
          error: function(XMLHttpRequest, textStatus, errorThrown) {
             var errorMessage = XMLHttpRequest.statusText;
             errorCallback(errorMessage);
          }
        })
    },
    login: function(username, password, successCallback, errorCallback) {
        $.post({
          url: '../api/login',
          contentType: 'application/json',
          dataType: 'json',
          data: JSON.stringify({"name": username, "password": password}),
          success: function(result) {
             var isLoginSuccess = result.data;
             successCallback(username, isLoginSuccess);
          },
          error: function(XMLHttpRequest, textStatus, errorThrown) {
             var errorMessage = XMLHttpRequest.statusText;
             errorCallback(errorMessage);
          }
        })
    },
    logout: function(username, successCallback, errorCallback) {
        $.post({
          url: '../api/logout',
          contentType: 'application/json',
          dataType: 'json',
          data: username,
          success: function(result) {
             var isLogout = result.data;
             successCallback(username, isLogout);
          },
          error: function(XMLHttpRequest, textStatus, errorThrown) {
             var errorMessage = XMLHttpRequest.statusText;
             errorCallback(errorMessage);
          }
        })
    }
};