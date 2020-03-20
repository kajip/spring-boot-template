package jp.co.biglobe.isp;

import jp.co.biglobe.isp.properties.ServiceProperties;
import jp.co.biglobe.isp.util.graceful_shutdown.GracefulShutdownHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties({ ServiceProperties.class })
@EnableAsync
public class BootApplication {

    public static void main(String args[]) {
        ConfigurableApplicationContext context = SpringApplication.run(BootApplication.class, args);

        // シグナルハンドラの登録
        GracefulShutdownHandler.regist(context);
    }
}
