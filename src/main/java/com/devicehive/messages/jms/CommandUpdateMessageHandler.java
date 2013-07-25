package com.devicehive.messages.jms;

import java.io.IOException;

import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.MessageType;
import com.devicehive.messages.bus.MessageBus;
import com.devicehive.model.DeviceCommand;

@JMSDestinationDefinition(
        name = Constants.JMS_COMMAND_UPDATE_TOPIC,
        interfaceName = "javax.jms.Topic",
        destinationName = Constants.COMMAND_UPDATE_TOPIC_DESTINATION_NAME)
@MessageDriven(mappedName = Constants.JMS_COMMAND_UPDATE_TOPIC)
public class CommandUpdateMessageHandler implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(CommandUpdateMessageHandler.class);

    @Inject
    private MessageBus messageBus;

    public void onMessage(Message message) {
        try {
            ObjectMessage objectMessage = (ObjectMessage) message;
            DeviceCommand deviceCommand = (DeviceCommand) objectMessage.getObject();
            if (deviceCommand != null) {
                logger.debug("DeviceCommand update received: " + deviceCommand);
                messageBus.notify(MessageType.DEVICE_TO_CLIENT_UPDATE_COMMAND, deviceCommand);
            }
        }
        catch (JMSException e) {
            logger.error("[onMessage] Error processing command update. ", e);
        }
        catch (IOException e) {
            logger.error("[onMessage] Error processing command update. ", e);
        }
    }
}
