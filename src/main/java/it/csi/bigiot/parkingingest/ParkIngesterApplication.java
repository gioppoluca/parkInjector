package it.csi.bigiot.parkingingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

@SpringBootApplication
@EnableScheduling
@EnableEncryptableProperties
public class ParkIngesterApplication {
	private static final Logger log = LoggerFactory.getLogger(ParkIngesterApplication.class);
	 
	public static void main(String[] args) {
		SpringApplication.run(ParkIngesterApplication.class, args);
	}
}
