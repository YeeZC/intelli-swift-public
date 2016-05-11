/**
 * Created by wuk on 16/4/18.
 */
BIConf.PermissionManageView = BI.inherit(BI.View, {

    _defaultConfig: function () {
        return BI.extend(BIConf.PermissionManageView.superclass._defaultConfig.apply(this, arguments), {
            baseCls: ""
        })
    },
    _init: function () {
        BIConf.PermissionManageView.superclass._init.apply(this, arguments);
        this.tab.setVisible(false);
        BI.Utils.getAllGroupedPackagesTreeAsync(function (items) {
                this.packageTree.populate(items);
            }
        )

    },

    _render: function (vessel) {
        this.main = BI.createWidget({
            type: "bi.border",
            element: vessel,
            items: {
                west: {el: this._builtPackageTree(), width: 400},
                center: {el: this._buildAuthorityPane(), height: 500, width: 40}
            }
        });
    },
    load: function () {
    },

    local: function () {
        return true;
    },
    refresh: function () {
    },

    _builtPackageTree: function () {
        var self = this;
        this.packageTree = BI.createWidget({
            type: "bi.package_tree"
        });
        this.packageTree.on(BI.PackageTree.EVENT_CHANGE, function () {
            Data.SharingPool.put("packageId", JSON.parse(self.packageTree.getPackageIds()));
            self.tab.setVisible(true);
            /*根据是否为批量修改确定添加方式*/
            switch (self.packageTree.getSelectType()) {
                case BI.PackageTree.SelectType.SingleSelect:
                    self.tab.populate(JSON.parse(self.packageTree.getPackageIds())[0]);
                    self._setHeadTitle(JSON.parse(self.packageTree.getPackageIds()), self.packageTree.getSelectType());
                    break;
                case BI.PackageTree.SelectType.MultiSelect:
                    self._setHeadTitle('',self.packageTree.getSelectType())
                    self.tab.populate();
                    break;
            }

        });
        // this.packageTree.populate();
        return this.packageTree;
    },
    _buildAuthorityPane: function () {
        return this.pane = BI.createWidget({
            type: "bi.border",
            items: {
                north: {el: this._showTitle(), width: 40, height: 40},
                center: {
                    el: this._buildAuthorityTabs(),
                    top: 0,
                    left: 0,
                    right: 0,
                    bottom: -40,
                    height: 500,
                    width: 400
                }
            }
        })
    },


    _showTitle: function () {
        this.title = BI.createWidget({
            type: "bi.label",
            text: BI.i18nText('BI-Permissions_Setting')
        });
        return this.title;
    },
    //设置标题
    _setHeadTitle: function (packageId,selectType) {
        var self = this;
        var packStructure = Data.SharingPool.get("packStructure");
        switch (selectType){
            case BI.PackageTree.SelectType.SingleSelect:
                BI.each(packStructure, function (key) {
                    if (packageId == packStructure[key].id) {
                        self.title.setText((packStructure[key].text) + '  ' + BI.i18nText('BI-Permissions_Setting'));
                        return;
                    }
                })
                break;
            case BI.PackageTree.SelectType.MultiSelect:
                self.title.setText('配置'+ packageId.length()+BI.i18nText('BI-Permissions_Setting'));
                break;
        }

;
    },
    _buildAuthorityTabs: function () {
        var self = this;
        self.tab = BI.createWidget({
            type: "bi.authority_pane",
        });
        return this.tab;
    }
})
