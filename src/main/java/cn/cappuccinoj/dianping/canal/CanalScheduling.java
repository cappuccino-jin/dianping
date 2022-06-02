package cn.cappuccinoj.dianping.canal;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.alibaba.otter.canal.protocol.exception.CanalClientException;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @Author cappuccino
 * @Date 2022-06-02 23:46
 */
@Component
public class CanalScheduling implements Runnable, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Resource
    private CanalConnector canalConnector;

    /**
     * fixedDelay = 100 每隔100毫秒,Spring 唤起该 run 方法执行代码逻辑
     */
    @Override
    @Scheduled(fixedDelay = 100)
    public void run() {
        long batchId = -1;
        try {
            // 一次拉取1000条消息数
            int batchSize = 1000;
            // 调用次方法需要手动 ack 告知canal
            Message message = canalConnector.getWithoutAck(batchSize);
            batchId = message.getId();
            List<CanalEntry.Entry> entries = message.getEntries();
            if (batchId != -1 && entries.size() > 0) {
                entries.forEach(entry -> {
                    // canal 只能处理 binlog row 方式做记录, entry 类型校验
                    if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
                        // 解析处理
                        publishCanalEvent(entry);
                    }
                });
            }
            // 告知此 batchId 已经处理完成
            canalConnector.ack(batchId);
        } catch (CanalClientException e) {
            e.printStackTrace();
            canalConnector.rollback(batchId);
        }
    }

    /**
     * 解析处理
     * @param entry
     */
    private void publishCanalEvent(CanalEntry.Entry entry){
        CanalEntry.EventType eventType = entry.getHeader().getEventType();
        String dataBase = entry.getHeader().getSchemaName();
        String table = entry.getHeader().getTableName();
        // entry 记录发生哪些类型变化
        CanalEntry.RowChange change = null;

        try {
            change = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return;
        }

        change.getRowDatasList().forEach(rowData -> {
            List<CanalEntry.Column> columns = rowData.getAfterColumnsList();
            String primaryKey = "id";
            // column.getIsKey(): 唯一索引, 主键或非唯一索引
            CanalEntry.Column idColumn = columns.stream()
                    .filter(column -> column.getIsKey() && primaryKey.equals(column.getName()))
                    .findFirst()
                    .orElse(null);

            Map<String, Object> jsonMap = parseColumnsToMap(columns);
            this.indexES(jsonMap, dataBase, table);
        });

    }

    /**
     * 将CanalEntry.Column 转成 Map: 列名为 key ,列值为 value
     *
     * @param columns
     * @return
     */
    Map<String, Object> parseColumnsToMap(List<CanalEntry.Column> columns) {
        Map<String, Object> jsonMap = Maps.newHashMap();
        columns.forEach(column -> {
            if (column == null) {
                return;
            }
            jsonMap.put(column.getName(), column.getValue());
        });
        return jsonMap;
    }

    private void indexES(Map<String, Object> dataMap, String dataBase, String table){
        if(!StringUtils.equals("dianping", dataBase)){
            return;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
