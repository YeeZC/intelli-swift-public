/**
 * Created by zcf on 2016/11/25.
 */
ExcelVDisplayManagerView = BI.inherit(BI.View, {

    _defaultConfig: function () {
        return BI.extend(ExcelVDisplayManagerView.superclass._defaultConfig.apply(this, arguments), {
            baseCls: ""
        })
    },

    _init: function () {
        ExcelVDisplayManagerView.superclass._init.apply(this, arguments);
    },

    _render: function (vessel) {
        var self = this;

        var uploadButton = BI.createWidget({
            type: "bi.upload_excel_button",
            text: BI.i18nText("BI-Upload_Data"),
            progressEL: this.excel,
            width: 120,
            height: 28
        });

        uploadButton.on(BI.UploadExcelButton.EVENT_AFTER_UPLOAD, function (files) {
            var file = files[files.length - 1];
            self.excelId = file.attach_id;
        });
        var excel = BI.createWidget({
            type: "bi.excel_view_display_manager"
        });

        var button = BI.createWidget({
            type: "bi.button",
            text: "populate",
            height: 30,
            width: 30
        });
        button.on(BI.Button.EVENT_CHANGE, function () {
            excel.setExcelId(self.excelId);
            excel.populate();
        });
        BI.createWidget({
            type: "bi.vertical",
            element: vessel,
            items: [uploadButton, button, excel]
        })
    }

});

ExcelVDisplayManagerModel = BI.inherit(BI.Model, {});