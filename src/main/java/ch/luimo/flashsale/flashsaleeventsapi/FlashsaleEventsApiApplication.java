package ch.luimo.flashsale.flashsaleeventsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class FlashsaleEventsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashsaleEventsApiApplication.class, args);
    }
}

