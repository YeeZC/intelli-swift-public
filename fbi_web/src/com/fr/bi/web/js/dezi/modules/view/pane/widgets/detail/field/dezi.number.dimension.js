BIDezi.NumberDimensionView = BI.inherit(BI.View, {

    constants: {
        DIMENSION_BUTTON_HEIGHT: 25,
        COMBO_WIDTH: 25
    },

    _defaultConfig: function () {
        return BI.extend(BIDezi.NumberDimensionView.superclass._defaultConfig.apply(this, arguments), {
            baseCls: "bi-dimension"
        })
    },

    _init: function () {
        BIDezi.NumberDimensionView.superclass._init.apply(this, arguments);
    },

    _vessel: function () {
        return this.element;
    },

    change: function (changed) {

    },

    _render: function (vessel) {
        var self = this;
        this.editor = BI.createWidget({
            type: "bi.sign_editor",
            height: this.constants.DIMENSION_BUTTON_HEIGHT,
            cls: "bi-dimension-name",
            allowBlank: false,
            validationChecker: function () {
                return self._checkDimensionName(self.editor.getValue());
            }
        });
        this.editor.on(BI.SignEditor.EVENT_CONFIRM, function () {
            self.model.set("name", self.editor.getValue());
        });

        this._createCombo();

        BI.createWidget({
            type: "bi.border",
            element: vessel,
            items: {
                east: {el: this.combo, width: this.constants.COMBO_WIDTH},
                center: {el: this.editor}
            }
        });
        var tableId = BI.Utils.getTableIdByFieldID(this.model.get("_src").field_id);
        this.editor.setValue(BI.Utils.getTableNameByID(tableId) + "." + this.model.get("name"));
    },

    _createCombo: function () {
        var self = this;
        this.combo = BI.createWidget({
            type: "bi.control_dimension_combo",
            dId: this.model.get("id")
        });
        this.combo.on(BI.ControlDimensionCombo.EVENT_CHANGE, function (v) {
            switch (v) {
                case BICst.CONTROL_COMBO.DELETE:
                    self._deleteDimension();
                    break;
            }
        });
    },

    _deleteDimension: function () {
        this.model.destroy();
    },

    _checkDimensionName: function (name) {
        var currId = this.model.get("id");
        var widgetId = BI.Utils.getWidgetIDByDimensionID(currId);
        var dimsId = BI.Utils.getAllDimensionIDs(widgetId);
        var valid = true;
        BI.some(dimsId, function (i, id) {
            if (currId !== id && BI.Utils.getDimensionNameByID(id) === name) {
                valid = false;
                return true;
            }
        });
        return valid;
    },

    refresh: function(){
        this.editor.setValue(this.model.get("name"));
        this.editor.setState(this.model.get("name"));
    }
});