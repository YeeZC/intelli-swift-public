package com.fr.swift.event.history;

import com.fr.swift.source.SourceKey;

/**
 * @author yee
 * @date 2018/6/8
 */
public class HistoryLoadSegmentRpcEvent extends SegmentLoadRpcEvent<SourceKey> {

    private static final long serialVersionUID = 5999241318201878252L;

    private SourceKey sourceKey;

    public HistoryLoadSegmentRpcEvent(SourceKey sourceKey, String sourceClusterId) {
        super(sourceClusterId);
        this.sourceKey = sourceKey;
    }

    public HistoryLoadSegmentRpcEvent() {
    }

    @Override
    public Event subEvent() {
        return Event.LOAD_SEGMENT;
    }

    @Override
    public SourceKey getContent() {
        return sourceKey;
    }
}
