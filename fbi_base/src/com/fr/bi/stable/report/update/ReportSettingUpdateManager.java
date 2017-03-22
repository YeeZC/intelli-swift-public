package com.fr.bi.stable.report.update;

import com.finebi.cube.common.log.BILoggerFactory;
import com.fr.bi.exception.BIReportVersionAbsentException;
import com.fr.bi.fs.BIDesignSetting;
import com.fr.bi.stable.constant.BIReportConstant;
import com.fr.bi.stable.utils.program.BIStringUtils;
import com.fr.general.ComparatorUtils;
import com.fr.json.JSONException;
import com.fr.json.JSONObject;

import java.util.*;

/**
 * Created by Kary on 2017/2/4.
 */
public class ReportSettingUpdateManager {
    private static ReportSettingUpdateManager ourInstance = new ReportSettingUpdateManager();

    public static ReportSettingUpdateManager getInstance() {
        return ourInstance;
    }

    private List<ReportConfVersionNode> versionNodes = new ArrayList<ReportConfVersionNode>();

    //
    protected ReportSettingUpdateManager() {
        initial();
    }

    public BIDesignSetting updateReportSettings(BIDesignSetting setting) throws Exception {
        JSONObject reportSettings = setting.getReportJSON();
        Iterator<ReportConfVersionNode> iterator = versionNodes.iterator();
        while (iterator.hasNext()) {
            ReportConfVersionNode node = iterator.next();
//            boolean flag = fileVersion < node.getVersion() && node.getVersion() <= Double.valueOf(BIReportConstant.VERSION);
            boolean flag = getVersion(setting).getVersionSort() < node.getVersion().getVersionSort();
            if (flag) {
                BILoggerFactory.getLogger(this.getClass()).info(BIStringUtils.append("profile files is updating ", getVersion(setting).getVersionName() + "------>" + node.getVersion()));
                reportSettings = node.getReportOperation().update(reportSettings);
            }
        }
        return new BIDesignSetting(reportSettings.toString());
    }

    /***
     *
     * @return
     * @throws Exception 该版本号无法在历史记录里找到
     * 处理出现未记录版本号的情况
     */
    public ReportVersion getVersion(BIDesignSetting setting) throws BIReportVersionAbsentException, JSONException {
        JSONObject reportSettings = setting.getReportJSON();
        if (null == reportSettings || !reportSettings.has("version")) {
            return new ReportVersion(BIReportConstant.HISTORY_VERSION.VERSION_4_0, Double.valueOf(BIReportConstant.HISTORY_VERSION.VERSION_4_0));
        }
        for (ReportConfVersionNode node : versionNodes) {
            if (ComparatorUtils.equals(reportSettings.getString("version") ,node.getVersion().getVersionName())) {
                return node.getVersion();
            }
        }
        throw new BIReportVersionAbsentException("version: " + reportSettings.get("version") + "not found");
    }

    private void initial() {
        try {
            if (null == versionNodes || versionNodes.size() == 0) {
                initialNodeList();
            }
        } catch (Exception e) {
            BILoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
        }
    }

    private void initialNodeList() throws Exception {
        versionNodes = ReportVersionNodeFactory.createVersionNodes();
    }

    protected void reSetNodeList(List nodeList) throws Exception {
        this.versionNodes = nodeList;
        Collections.sort(versionNodes);
    }
}
