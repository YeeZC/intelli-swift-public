/**
 * Created by 小灰灰 on 2016/4/7.
 */
BI.AnalysisETLMainModel = BI.inherit(BI.MVCModel, {
    _init : function () {
        BI.AnalysisETLMainModel.superclass._init.apply(this, arguments);
        this.set(BI.AnalysisETLMainModel.TAB, new BI.DynamictabModel(this.get(BI.AnalysisETLMainModel.TAB)));
    },

    getSheetLength : function () {
        return this.get(BI.AnalysisETLMainModel.TAB).get(SQLCst.ITEMS).length;
    },

    getTableDefaultName : function () {
        var id = this.get(BI.AnalysisETLMainModel.TAB).get(SQLCst.ITEMS)[0];
        var name = this.get('name') || this.get(BI.AnalysisETLMainModel.TAB).get(id).get('table_name');
        return BI.Utils.createSQLDistinctName(BI.Utils.getAllSQLTableNames(), name);
    },
    
    update : function () {
        var value = {
            id : this.get('id'),
            name : this.get('name'),
            describe : this.get('describe')
        }
        value[BI.AnalysisETLMainModel.TAB] = this.get(BI.AnalysisETLMainModel.TAB).update();
        return value;
    }
});
BI.AnalysisETLMainModel.TAB = 'table';