package com.fr.swift.conf.business.relation;

import com.finebi.base.constant.FineEngineType;
import com.finebi.base.utils.data.io.FileUtils;
import com.finebi.conf.service.dao.provider.BusinessConfigDAO;
import com.finebi.conf.structure.relation.FineBusinessTableRelation;
import com.fr.general.ComparatorUtils;
import com.fr.swift.conf.business.AbstractSwiftParseXml;
import com.fr.swift.conf.business.ISwiftXmlWriter;
import com.fr.swift.conf.business.container.ResourceContainer;
import com.fr.swift.conf.business.table.SwiftTableDao;
import com.fr.swift.log.SwiftLogger;
import com.fr.swift.log.SwiftLoggers;
import com.fr.third.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class created on 2018-1-24 10:02:02
 *
 * @author Lucifer
 * @description
 * @since Advanced FineBI Analysis 1.0
 */
public class SwiftRelationDao implements BusinessConfigDAO<FineBusinessTableRelation> {

    private final static String XML_FILE_NAME = "relation.xml";
    private static final SwiftLogger LOGGER = SwiftLoggers.getLogger(SwiftRelationDao.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    private AbstractSwiftParseXml xmlHandler;
    private ISwiftXmlWriter swiftXmlWriter;
    private String xmlFileName;
    private SwiftTableDao tableDao;
    private ResourceContainer<FineBusinessTableRelation> container = RelationContainer.getContainer();


    public SwiftRelationDao(AbstractSwiftParseXml xmlHandler, String xmlFileName, ISwiftXmlWriter swiftXmlWriter) {
        this.xmlHandler = xmlHandler;
        this.xmlFileName = xmlFileName;
        this.swiftXmlWriter = swiftXmlWriter;
        this.tableDao = new SwiftTableDao(this.xmlHandler, this.xmlFileName, this.swiftXmlWriter);
    }

    @Override
    public boolean saveConfig(FineBusinessTableRelation tableRelation) {
        List<FineBusinessTableRelation> tableRelations = new ArrayList<FineBusinessTableRelation>();
        tableRelations.add(tableRelation);
        return this.saveConfigs(tableRelations);
    }

    @Override
    public boolean saveConfigs(List<FineBusinessTableRelation> tableRelationList) {
        try {
            List<FineBusinessTableRelation> relations = getAllConfig();
//            Map<String, FineBusinessTableRelation> relationMap = convertToMap(relations);
//            Map<String, FineBusinessTableRelation> saveRelationMap = convertToMap(tableRelationList);
//            relationMap.putAll(saveRelationMap);
            relations.addAll(tableRelationList);
            container.saveResources(relations);
            return true;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean updateConfig(FineBusinessTableRelation tableRelation) {
        List<FineBusinessTableRelation> tableRelationList = new ArrayList<FineBusinessTableRelation>();
        tableRelationList.add(tableRelation);
        return this.updateConfigs(tableRelationList);
    }

    @Override
    public boolean updateConfigs(List<FineBusinessTableRelation> tableRelationList) {
        List<FineBusinessTableRelation> resources = getAllConfig();
        Iterator<FineBusinessTableRelation> it = resources.iterator();
        while (it.hasNext()) {
            FineBusinessTableRelation fineResource = it.next();
            for (FineBusinessTableRelation resource : tableRelationList) {
                if (ComparatorUtils.equals(resource.getRelationName(), fineResource.getRelationName())) {
                    it.remove();
                    break;
                }
            }
        }
        resources.addAll(tableRelationList);
        container.saveResources(resources);
        return true;
    }

    @Override
    public boolean removeConfig(FineBusinessTableRelation tableRelation) {
        List<FineBusinessTableRelation> tableRelationList = new ArrayList<FineBusinessTableRelation>();
        tableRelationList.add(tableRelation);
        return this.removeConfigs(tableRelationList);
    }

    @Override
    public boolean removeConfigs(List<FineBusinessTableRelation> tableRelationList) {
        try {
            List<FineBusinessTableRelation> resources = getAllConfig();
            Iterator<FineBusinessTableRelation> it = resources.iterator();
            while (it.hasNext()) {
                FineBusinessTableRelation fineResource = it.next();
                for (FineBusinessTableRelation resource : tableRelationList) {
                    if (ComparatorUtils.equals(resource.getRelationName(), fineResource.getRelationName())) {
                        it.remove();
                        break;
                    }
                }
            }
            container.saveResources(resources);
            return true;
        } catch (Exception e) {
            LOGGER.info(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean removeAllConfig() {
//        try {
////            saveRelations(new HashMap<String, FineBusinessTableRelation>());
////            return true;
////        } catch (Exception e) {
////            LOGGER.error(e.getMessage(), e);
////            return false;
////        }
        try {
            container.saveResources(new ArrayList<FineBusinessTableRelation>());
            return true;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public List<FineBusinessTableRelation> getAllConfig() {
//        try {
//            Map<String, Map> map = objectMapper.readValue(makeSureXmlFileExist(), Map.class);
//            List<FineBusinessTable> businessTableList = tableDao.getAllConfig();
//            List<FineBusinessTableRelation> result = SimpleRelationUtils.transforSimpleRelarions(map, businessTableList);
//            return result;
//        } catch (Exception e) {
//            LOGGER.error(e.getMessage(), e);
//        }
//        return new ArrayList<FineBusinessTableRelation>();
        return container.getResources();
    }

    @Override
    public FineEngineType getEngineType() {
        return FineEngineType.Cube;
    }

    /**
     * 判断文件是否存在，不存在的话，创建一个文件
     */
    private File makeSureXmlFileExist() {
        String path = this.getClass().getResource("/").getPath();
        File tableFile = new File(path + File.separator + XML_FILE_NAME);
        if (!tableFile.exists()) {
            FileUtils.createFile(tableFile);
        }
        return tableFile;
    }

    private boolean saveRelations(Map<String, FineBusinessTableRelation> relationMap) {
        try {
            Map<String, SimpleBusinessRelation> simpleBusinessRelationMap = new HashMap<String, SimpleBusinessRelation>();
            for (Map.Entry<String, FineBusinessTableRelation> entry : relationMap.entrySet()) {
                SimpleBusinessRelation simpleBusinessRelation = SimpleRelationUtils.transformRelation(entry.getValue());
                simpleBusinessRelationMap.put(simpleBusinessRelation.getRelationName(), simpleBusinessRelation);
            }
            objectMapper.writeValue(makeSureXmlFileExist(), simpleBusinessRelationMap);
            return true;
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    private Map<String, FineBusinessTableRelation> convertToMap(List<FineBusinessTableRelation> relationList) {
        Map<String, FineBusinessTableRelation> relations = new HashMap<String, FineBusinessTableRelation>();
        for (FineBusinessTableRelation relation : relationList) {
            relations.put(relation.getRelationName(), relation);
        }
        return relations;
    }

    public List<FineBusinessTableRelation> getRelation(String primary, String foreign) {
        List<FineBusinessTableRelation> allConfigs = getAllConfig();
        List<FineBusinessTableRelation> result = new ArrayList<FineBusinessTableRelation>();
        for (FineBusinessTableRelation relation :
                allConfigs) {
            if (primary.equals(relation.getPrimaryBusinessTable().getName()) && foreign.equals(relation.getForeignBusinessTable().getName())) {
                result.add(relation);
            }
        }
        return result;
    }
}
