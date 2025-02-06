package io.lazydog;

import java.util.ArrayList;
import java.util.List;

public class PlatformDataAggregator {

    public static class PlatformQueryInfo {
        private String platformId;
        private int pageNum;
        private int pageSize;

        public PlatformQueryInfo(String platformId, int pageNum, int pageSize) {
            this.platformId = platformId;
            this.pageNum = pageNum;
            this.pageSize = pageSize;
        }

        public String getPlatformId() {
            return platformId;
        }

        public int getPageNum() {
            return pageNum;
        }

        public int getPageSize() {
            return pageSize;
        }

        @Override
        public String toString() {
            return "{PlatformId='" + platformId + "', PageNum=" + pageNum + ", PageSize=" + pageSize + "}";
        }
    }

    public static List<PlatformQueryInfo> getPlatformsToQuery(int pageNum, int pageSize) {
        int size_A = 60;
        int size_B = 70;
        int size_C = 80;
        int size_D = 90;

        int[] platformSizes = {size_A, size_B, size_C, size_D};
        String[] platformIds = {"A", "B", "C", "D"};
        int[] cumulativeSizes = new int[platformSizes.length];
        cumulativeSizes[0] = platformSizes[0];
        for (int i = 1; i < platformSizes.length; i++) {
            cumulativeSizes[i] = cumulativeSizes[i - 1] + platformSizes[i];
        }

        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = startIndex + pageSize - 1;

        List<PlatformQueryInfo> platformsToQuery = new ArrayList<>();
        int previousCumulativeSize = 0;

        for (int i = 0; i < platformIds.length; i++) {
            String platformId = platformIds[i];
            int platformSize = platformSizes[i];
            int cumulativeSize = cumulativeSizes[i];

            int platformStartIndexRange = previousCumulativeSize;
            int platformEndIndexRange = cumulativeSize - 1;

            if (Math.max(startIndex, platformStartIndexRange) <= Math.min(endIndex, platformEndIndexRange)) {
                int queryStartGlobalIndex = Math.max(startIndex, platformStartIndexRange);
                int queryEndGlobalIndex = Math.min(endIndex, platformEndIndexRange);
                int querySize = queryEndGlobalIndex - queryStartGlobalIndex + 1;
                platformsToQuery.add(new PlatformQueryInfo(platformId, 1, 50)); // PageNum is always 1 for the platform query
            }
            previousCumulativeSize = cumulativeSize;
        }
        return platformsToQuery;
    }

    public static void main(String[] args) {
        int pageNum = 3;
        int pageSize = 110;
        List<PlatformQueryInfo> queryInfoList = getPlatformsToQuery(pageNum, pageSize);

        System.out.println("对于第 " + pageNum + " 页 (每页 " + pageSize + " 条数据), 需要查询的平台信息如下:");
        if (queryInfoList.isEmpty()) {
            System.out.println("无需查询任何平台 (已超出总数据范围).");
        } else {
            for (PlatformQueryInfo queryInfo : queryInfoList) {
                System.out.println(queryInfo);
            }
        }

        System.out.println("\n边界问题测试:");
        System.out.println("第一页 (PageNum = 1): " + getPlatformsToQuery(1, 50)); // 平台A
        System.out.println("第二页 (PageNum = 2): " + getPlatformsToQuery(2, 50)); // 平台A，平台B
        System.out.println("第三页 (PageNum = 3): " + getPlatformsToQuery(3, 50)); // 平台B，平台C
        System.out.println("第四页 (PageNum = 4): " + getPlatformsToQuery(4, 50)); // 平台C
        System.out.println("第五页 (PageNum = 5): " + getPlatformsToQuery(5, 50)); // 平台C，平台D
        System.out.println("第六页 (PageNum = 6): " + getPlatformsToQuery(6, 50)); // 平台D
        System.out.println("第七页 (PageNum = 7): " + getPlatformsToQuery(7, 50)); // 平台D
        System.out.println("第八页 (PageNum = 8): " + getPlatformsToQuery(8, 50)); // 无平台，超出总数据量
    }
}