package cn.cappuccinoj.dianping.canal;

import com.alibaba.google.common.collect.Lists;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * @Author cappuccino
 * @Date 2022-06-02 19:35
 */
@Component
public class CanalClient implements DisposableBean {

    private CanalConnector canalConnector;

    @Bean
    public CanalConnector getCanalConnector(){
        canalConnector = CanalConnectors.newClusterConnector(Lists.newArrayList(new InetSocketAddress("127.0.0.1", 11111)),
                "example", "canal", "canal"
        );
        // 连接 canal
        canalConnector.connect();
        // 指定filter, 格式{database}.{table}
        canalConnector.subscribe();
        // 回滚寻找上次中断的位置
        canalConnector.rollback();
        return canalConnector;
    }

    @Override
    public void destroy() throws Exception {
        if (null != canalConnector) {
            // 连接中断, 防止连接泄漏
            canalConnector.disconnect();
        }
    }
}
