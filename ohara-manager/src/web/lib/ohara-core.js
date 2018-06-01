
(function( $ ) {

    // TODO: Custom Named Events
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
                $baseEl: $baseEl
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

            if (result.clickBinding) {
                $.each(result.clickBinding, _bindClickEvent);
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
    login: function(username, password, successCallback, errorCallback) {
        $.post({
          url: '../api/login',
          contentType: 'application/json',
          dataType: 'json',
          data: JSON.stringify({"name": username, "password": password}),
          success: function(result) {
             var isLoginSuccess = result.data
             successCallback(username, isLoginSuccess)
          },
          error: function(XMLHttpRequest, textStatus, errorThrown) {
             var errorMessage = XMLHttpRequest.statusText
             errorCallback(errorMessage)
          }
        })
    }
};