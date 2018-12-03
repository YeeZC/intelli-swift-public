package com.fr.swift.config.entity;

import com.fr.swift.config.SwiftConfigConstants;
import com.fr.swift.config.bean.SegmentKeyBean;
import com.fr.swift.config.convert.FRURIConverter;
import com.fr.swift.converter.ObjectConverter;
import com.fr.swift.cube.io.Types;
import com.fr.swift.cube.io.Types.StoreType;
import com.fr.swift.db.SwiftDatabase;
import com.fr.swift.segment.SegmentKey;
import com.fr.swift.source.SourceKey;
import com.fr.third.javax.persistence.Column;
import com.fr.third.javax.persistence.Convert;
import com.fr.third.javax.persistence.Entity;
import com.fr.third.javax.persistence.EnumType;
import com.fr.third.javax.persistence.Enumerated;
import com.fr.third.javax.persistence.Id;
import com.fr.third.javax.persistence.Table;

import java.net.URI;

/**
 * @author yee
 * @date 2018/5/24
 */
@Entity
@Table(name = "fine_swift_segments")
public class SwiftSegmentEntity implements ObjectConverter<SegmentKeyBean> {
    @Id
    private String id;

    @Column(name = SwiftConfigConstants.SegmentConfig.COLUMN_SEGMENT_OWNER)
    private String segmentOwner;

    @Column(name = SwiftConfigConstants.SegmentConfig.COLUMN_SEGMENT_URI, length = SwiftConfigConstants.LONG_TEXT_LENGTH)
    @Convert(
            converter = FRURIConverter.class
    )
    private URI segmentUri;

    @Column(name = SwiftConfigConstants.SegmentConfig.COLUMN_SEGMENT_ORDER)
    private int segmentOrder;

    @Column(name = SwiftConfigConstants.SegmentConfig.COLUMN_STORE_TYPE)
    @Enumerated(EnumType.STRING)
    private Types.StoreType storeType;

    @Column(name = "swiftSchema")
    @Enumerated(EnumType.STRING)
    private SwiftDatabase swiftSchema;

    public SwiftSegmentEntity() {
    }

    public SwiftSegmentEntity(SegmentKey segKey) {
        this(segKey.getTable(), segKey.getOrder(), segKey.getStoreType(), segKey.getSwiftSchema());
    }

    public SwiftSegmentEntity(SourceKey segmentOwner, int segmentOrder, StoreType storeType, SwiftDatabase swiftSchema) {
        id = getId(segmentOwner, segmentOrder, storeType);
        this.segmentOwner = segmentOwner.getId();
        this.segmentUri = URI.create(String.format("%s/seg%d", segmentOwner.getId(), segmentOrder));
        this.segmentOrder = segmentOrder;
        this.storeType = storeType;
        this.swiftSchema = swiftSchema;
    }

    private static String getId(SourceKey segmentOwner, int segmentOrder, StoreType storeType) {
        return String.format("%s@%s@%d", segmentOwner.getId(), storeType, segmentOrder);
    }

    public String getSegmentOwner() {
        return segmentOwner;
    }

    public void setSegmentOwner(String segmentOwner) {
        this.segmentOwner = segmentOwner;
    }

    public URI getSegmentUri() {
        return segmentUri;
    }

    public void setSegmentUri(URI segmentUri) {
        this.segmentUri = segmentUri;
    }

    public int getSegmentOrder() {
        return segmentOrder;
    }

    public void setSegmentOrder(int segmentOrder) {
        this.segmentOrder = segmentOrder;
    }

    public StoreType getStoreType() {
        return storeType;
    }

    public void setStoreType(StoreType storeType) {
        this.storeType = storeType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SwiftDatabase getSwiftSchema() {
        return swiftSchema;
    }

    public void setSwiftSchema(SwiftDatabase swiftSchema) {
        this.swiftSchema = swiftSchema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SwiftSegmentEntity entity = (SwiftSegmentEntity) o;

        if (segmentOrder != entity.segmentOrder) {
            return false;
        }
        if (id != null ? !id.equals(entity.id) : entity.id != null) {
            return false;
        }
        if (segmentOwner != null ? !segmentOwner.equals(entity.segmentOwner) : entity.segmentOwner != null) {
            return false;
        }
        if (segmentUri != null ? !segmentUri.equals(entity.segmentUri) : entity.segmentUri != null) {
            return false;
        }
        if (storeType != entity.storeType) {
            return false;
        }
        return swiftSchema == entity.swiftSchema;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (segmentOwner != null ? segmentOwner.hashCode() : 0);
        result = 31 * result + (segmentUri != null ? segmentUri.hashCode() : 0);
        result = 31 * result + segmentOrder;
        result = 31 * result + (storeType != null ? storeType.hashCode() : 0);
        result = 31 * result + (swiftSchema != null ? swiftSchema.hashCode() : 0);
        return result;
    }

    @Override
    public SegmentKeyBean convert() {
        return new SegmentKeyBean(segmentOwner, segmentUri, segmentOrder, storeType, swiftSchema);
    }
}
