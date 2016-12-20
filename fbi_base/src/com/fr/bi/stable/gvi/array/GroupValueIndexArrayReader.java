package com.fr.bi.stable.gvi.array;

import com.fr.bi.common.inter.ValueCreator;
import com.fr.bi.stable.gvi.ByteArrayDataInput;
import com.fr.bi.stable.gvi.GVIFactory;
import com.fr.bi.stable.gvi.GroupValueIndex;
import com.fr.bi.stable.io.newio.NIOReader;
import com.fr.bi.stable.structure.collection.map.lru.LRUWithKHashMap;

public class GroupValueIndexArrayReader implements NIOReader<GroupValueIndex>, ICubeTableIndexReader {
    private static final int MAX_CACHE_SIZE = 1024;

    private NIOReader<byte[]> byteList;

    private LRUWithKHashMap<Long, GroupValueIndex> tmpMap;

    public GroupValueIndexArrayReader(NIOReader<byte[]> byteList) {
        this.byteList = byteList;
        tmpMap = new LRUWithKHashMap<Long, GroupValueIndex>((int) (MAX_CACHE_SIZE >> 2));
    }

    @Override
    public GroupValueIndex get(final long row) {
        return tmpMap.get(row, new ValueCreator<GroupValueIndex>() {

            @Override
            public GroupValueIndex createNewObject() {
                byte[] b = byteList.get(row);
                GroupValueIndex result = GVIFactory.createGroupValueIndexByDataInput(new ByteArrayDataInput(b));
                return result;
            }
        });
    }

    @Override
    public int getReverse(int row) {
        return 0;
    }

    @Override
    public GroupValueIndex getNullIndex() {
        return GVIFactory.createAllEmptyIndexGVI();
    }

    @Override
    public long getLastPos(long rowCount) {
        return 0;
    }

    @Override
    public void clear() {
    }

}