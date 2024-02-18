package jpabook2.jpashop2;

import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Jpashop2Application {

    public static void main(String[] args) {
        SpringApplication.run(Jpashop2Application.class, args);
    }

    @Bean
    Hibernate5JakartaModule hibernate5Module() {
        return new Hibernate5JakartaModule();
        // hibernate5Module().configure(Hibernate5JakartaModule.Feature.FORCE_LAZY_LOADING, true); => json 생성 시점에 지연로딩을 강제로 모두 실행함
        // 설정으로 인해서 원치 않게 orderItems도 노출됨 => 성능 저하 (쿼리가 item, category등에 대해서도 모두 나감)

    }
}
