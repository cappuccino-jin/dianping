package cn.cappuccinoj.dianping;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @Author cappuccino
 * @Date 2022-05-18 22:44
 */
@SpringBootApplication(scanBasePackages = {"cn.cappuccinoj.dianping"})
@MapperScan("cn.cappuccinoj.dianping.dao")
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class DianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DianPingApplication.class, args);
    }

}
