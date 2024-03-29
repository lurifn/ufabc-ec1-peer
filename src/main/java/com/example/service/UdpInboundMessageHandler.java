package com.example.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Headers;

import com.example.bo.GossipBO;

@MessageEndpoint
public class UdpInboundMessageHandler {
	
	@Autowired
	GossipBO bo;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(UdpInboundMessageHandler.class);
	
	@ServiceActivator(inputChannel = "inboundChannel")
	public void handeMessage(Message message, @Headers Map<String, Object> headerMap) {
		String mensagem = new String((byte[]) message.getPayload());
		
		LOGGER.info("Received UDP message: {}", mensagem);
		
		//atualiza o estado da máquina
		bo.atualizaEstado(mensagem);
	}
}
