package com.kyle.strategy.impl;

import com.kyle.strategy.IDBRouterStrategy;
import com.kyle.DBContextHolder;
import com.kyle.DBRouterConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DBRouterStrategyHashCode implements IDBRouterStrategy {

    private DBRouterConfig dbRouterConfig;

    public DBRouterStrategyHashCode(DBRouterConfig dbRouterConfig) {
        this.dbRouterConfig = dbRouterConfig;
    }

    /**
     * 计算方式:
     * size = 库*表的数量
     * idx : 散列到的哪张表
     * dbIdx = idx / dbRouterConfig.getTbCount() + 1;
     * dbIdx : 用于计算哪个库,idx为0-size的值，除以表的数量 = 当前是几号库，又因库是从一号库开始算的，因此需要+1
     * tbIdx : idx - dbRouterConfig.getTbCount() * (dbIdx - 1);用于计算哪个表，
     * idx 可以理解为是第X张表，但是需要落地到是第几个库的第几个表
     * 例子：假设2库8表，idx为14，因此是第二个库的第6个表才是第14张表
     * (dbIdx - 1) 因为库是从1开始算的，因此这里需要-1
     * dbRouterConfig.getTbCount() * (dbIdx - 1) 是为了算出当前库前面的多少张表，也就是要跳过前面的这些表，
     * 然后来计算当前库中的表
     *
     * @param dbKeyAttr 路由字段属性值
     *                  dbRouter.key() 确定根据哪个字段进行路由
     *                  getAttrValue 根据数据库路由字段，从入参中读取出对应的值。
     *                  比如路由 key 是 uId，那么就从入参对象 Obj 中获取到 uId 的值。
     */
    @Override
    public void doRouter(String dbKeyAttr) {
        //获取当前大小 db*tb
        int size = dbRouterConfig.getDbCount() * dbRouterConfig.getTbCount();

        //获取当前idx
        int idx = (size - 1) & (dbKeyAttr.hashCode() ^ (dbKeyAttr.hashCode() >>> 16));
        //假如当前表的数量为15
        // 0000 1110
        // 0000 1010 =>1010
        // 0000 0011 =>0010
        // 因为15张表的二进制就是1110 14 ，也就是说末位一定为0
        //这样导致无论hash值末位是0还是1都会变成0 这样就有一半的表用不到

        //假如当前表的数量为16
        // 0000 1111
        // 0000 1010 =>1010
        // 0000 0011 =>0011
        // 扰动函数；在 JDK 的 HashMap 中，对于一个元素的存放，需要进行哈希散列。而为了让散列更加均匀，所以添加了扰动函数。

        //找到idx属于哪个库 哪张表
        int dbIdx = idx / dbRouterConfig.getTbCount() + 1;

        int tbIdx = idx - (dbIdx - 1) * dbRouterConfig.getTbCount();
        // 借鉴了HashMap的扰动函数
       /* static final int hash(Object key) {
            int h;
            return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
        }*/
        // 设置库表信息到上下文,String.format("%02d", dbIdx),数据不为两位的话则在前面补0,这里的策略主要和设置的库表名称有关
        // 例如: 库名称为test_01 那就写%02d。表名称user_001 对应%03d
        DBContextHolder.setDBKey(String.format("%02d", dbIdx));
        DBContextHolder.setTBKey(String.format("%03d", tbIdx));
        log.debug("数据库路由 dbIdx：{} tbIdx：{}", dbIdx, tbIdx);

    }

    @Override
    public void setDBKey(int dbIdx) {
        DBContextHolder.setDBKey(String.format("%02d", dbIdx));
    }

    @Override
    public void setTBKey(int tbIdx) {
        DBContextHolder.setTBKey(String.format("%03d", tbIdx));
    }

    @Override
    public int dbCount() {
        return dbRouterConfig.getDbCount();
    }

    @Override
    public int tbCount() {
        return dbRouterConfig.getTbCount();
    }

    @Override
    public void clear() {
        DBContextHolder.clearDBKey();
        DBContextHolder.clearTBKey();
    }
}
