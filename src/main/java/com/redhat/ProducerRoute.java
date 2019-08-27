package com.redhat;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConfiguration;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ProducerRoute extends RouteBuilder {
	@Override
	public void configure() throws Exception {
		restConfiguration().component("servlet").bindingMode(RestBindingMode.json);
		
		KafkaComponent kafka = new KafkaComponent();		
		KafkaConfiguration kafkaConfig = new KafkaConfiguration();
		kafkaConfig.setBrokers("my-cluster-kafka-bootstrap-myproject.apps.akrohg-openshift.redhatgov.io:443");
		kafkaConfig.setSecurityProtocol("SSL");
		kafkaConfig.setSslTruststoreLocation("src/main/resources/keystore.jks");
		kafkaConfig.setSslTruststorePassword("password");
		kafka.setConfiguration(kafkaConfig);
		
		getContext().addComponent("kafka", kafka);
		
		rest("/rest")
		.post("/produce").route().streamCaching().process(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				Message message = exchange.getIn();
//				String username = message.getBody(Map.class).get("resourceType").toString().toLowerCase();
//				String direction = message.getBody(Map.class).get("resourceType").toString().toLowerCase();
				
				log.info("Received message: {}", message.getBody(String.class));
				
				message.setBody(new ObjectMapper().writeValueAsString(exchange.getIn().getBody()));
				message.setHeader(KafkaConstants.PARTITION_KEY, 0);
				message.setHeader(KafkaConstants.KEY, "Camel");
			}
		}).recipientList(simple("kafka:directive")).setBody(constant("Message sent successfully."));
	}
}