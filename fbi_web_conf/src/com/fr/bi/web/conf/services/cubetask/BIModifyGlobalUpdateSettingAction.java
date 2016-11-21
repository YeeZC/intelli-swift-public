package com.fr.bi.web.conf.services.cubetask;

import com.finebi.cube.common.log.BILoggerFactory;
import com.finebi.cube.conf.BICubeManagerProvider;
import com.fr.bi.cal.BICubeManager;
import com.fr.bi.conf.manager.update.source.UpdateSettingSource;
import com.fr.bi.conf.provider.BIConfigureManagerCenter;
import com.fr.bi.stable.constant.DBConstant;
import com.fr.bi.web.conf.AbstractBIConfigureAction;
import com.fr.fs.web.service.ServiceUtils;
import com.fr.json.JSONObject;
import com.fr.stable.bridge.StableFactory;
import com.fr.web.utils.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BIModifyGlobalUpdateSettingAction extends AbstractBIConfigureAction {
    @Override
    public String getCMD() {
        return "modify_global_update_setting_action";
    }

    @Override
    protected void actionCMDPrivilegePassed(HttpServletRequest req,
                                            HttpServletResponse res) throws Exception {
        long userId = ServiceUtils.getCurrentUserID(req);
        String globalUpdateSetting = WebUtils.getHTTPRequestParameter(req, "setting");
        UpdateSettingSource source = new UpdateSettingSource();
        source.parseJSON(new JSONObject(globalUpdateSetting));
        try {
            BIConfigureManagerCenter.getUpdateFrequencyManager().saveUpdateSetting(DBConstant.CUBE_UPDATE_TYPE.GLOBAL_UPDATE, source, userId);
            BIConfigureManagerCenter.getUpdateFrequencyManager().persistData(userId);
            StableFactory.getMarkedObject(BICubeManagerProvider.XML_TAG, BICubeManager.class).resetCubeGenerationHour(userId);
        } catch (Exception e) {
            BILoggerFactory.getLogger().error(e.getMessage());
        }
    }
}