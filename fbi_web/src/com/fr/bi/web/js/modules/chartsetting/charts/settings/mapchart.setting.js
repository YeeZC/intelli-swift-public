/**
 * @class BI.MapSetting
 * @extends BI.Widget
 * 柱状，堆积柱状，组合图样式
 */
BI.MapSetting = BI.inherit(BI.Widget, {
    constant: {
        SINGLE_LINE_HEIGHT: 60,
        SIMPLE_H_GAP: 10,
        SIMPLE_L_GAP: 2,
        CHECKBOX_WIDTH: 16,
        EDITOR_WIDTH: 80,
        EDITOR_HEIGHT: 26,
        BUTTON_WIDTH: 40,
        BUTTON_HEIGHT: 30,
        ICON_WIDTH: 24,
        ICON_HEIGHT: 24,
        NUMBER_LEVEL_SEGMENT_WIDTH: 300,
        FORMAT_SEGMENT_WIDTH: 240,
        LEGEND_SEGMENT_WIDTH: 180
    },

    _defaultConfig: function(){
        return BI.extend(BI.MapSetting.superclass._defaultConfig.apply(this, arguments), {
            baseCls: "bi-charts-setting"
        })
    },

    _init: function(){
        BI.MapSetting.superclass._init.apply(this, arguments);
        var self = this, o = this.options;

        //图例
        this.legend = BI.createWidget({
            type: "bi.segment",
            width: this.constant.LEGEND_SEGMENT_WIDTH,
            height: this.constant.BUTTON_HEIGHT,
            items: BICst.MAP_CHART_LEGEND
        });

        this.legend.on(BI.Segment.EVENT_CHANGE, function(){
            self.fireEvent(BI.MapSetting.EVENT_CHANGE);
        });

        //数据标签
        this.showDataLabel = BI.createWidget({
            type: "bi.multi_select_item",
            value: BI.i18nText("BI-Show_Data_Label"),
            width: 115
        });

        this.showDataLabel.on(BI.Controller.EVENT_CHANGE, function(){
            self.fireEvent(BI.MapSetting.EVENT_CHANGE);
        });

        var showElement = BI.createWidget({
            type: "bi.horizontal",
            cls: "single-line-settings",
            lgap: this.constant.SIMPLE_H_GAP,
            items: [{
                type: "bi.label",
                text: BI.i18nText("BI-Element_Show"),
                textHeight: 60,
                cls: "line-title"
            }, {
                type: "bi.left",
                cls: "detail-style",
                items: BI.createItems([{
                    type: "bi.label",
                    text: BI.i18nText("BI-Legend_Normal"),
                    lgap: this.constant.SIMPLE_H_GAP,
                    cls: "attr-names"
                }, {
                    type: "bi.center_adapt",
                    items: [this.legend]
                }, {
                    type: "bi.center_adapt",
                    items: [this.showDataLabel]
                }, {
                    type: "bi.center_adapt",
                    items: [this.showDataTable]
                }, {
                    type: "bi.center_adapt",
                    items: [this.gridLine]
                }, {
                    type: "bi.center_adapt",
                    items: [this.showZoom]
                }, {
                    type: "bi.center_adapt",
                    items: [this.nullContinue]
                }], {
                    height: this.constant.SINGLE_LINE_HEIGHT
                }),
                lgap: this.constant.SIMPLE_H_GAP
            }]
        });

        //联动传递指标过滤条件
        this.transferFilter = BI.createWidget({
            type: "bi.multi_select_item",
            value: BI.i18nText("BI-Bind_Target_Condition"),
            width: 170
        });
        this.transferFilter.on(BI.Controller.EVENT_CHANGE, function(){
            self.fireEvent(BI.GroupTableSetting.EVENT_CHANGE);
        });

        var otherAttr = BI.createWidget({
            type: "bi.left_right_vertical_adapt",
            cls: "single-line-settings",
            items: {
                left: [{
                    type: "bi.label",
                    text: BI.i18nText("BI-Interactive_Attr"),
                    cls: "line-title"
                }, this.transferFilter]
            },
            height: this.constant.SINGLE_LINE_HEIGHT,
            lhgap: this.constant.SIMPLE_H_GAP
        });

        BI.createWidget({
            type: "bi.vertical",
            element: this.element,
            items: [showElement, otherAttr],
            hgap: 10
        })
    },

    populate: function(){
        var wId = this.options.wId;
        this.transferFilter.setSelected(BI.Utils.getWSTransferFilterByID(wId));
        this.legend.setValue(BI.Utils.getWSChartLegendByID(wId));
        this.showDataLabel.setSelected(BI.Utils.getWSShowDataLabelByID(wId));
    },

    getValue: function(){
        return {
            transfer_filter: this.transferFilter.isSelected(),
            chart_legend: this.legend.getValue()[0],
            show_data_label: this.showDataLabel.isSelected()
        }
    },

    setValue: function(v){
        this.transferFilter.setSelected(v.transfer_filter);
        this.legend.setValue(v.chart_legend);
        this.showDataLabel.setSelected(v.show_data_label);
    }
});
BI.MapSetting.EVENT_CHANGE = "EVENT_CHANGE";
$.shortcut("bi.map_setting", BI.MapSetting);