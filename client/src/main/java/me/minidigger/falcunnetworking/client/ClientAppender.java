package me.minidigger.falcunnetworking.client;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;

@Plugin(name = "ClientAppender", category = "Core", elementType = Appender.ELEMENT_TYPE, printObject = true)
public class ClientAppender extends AbstractAppender {

    public ClientAppender(String name, Layout<?> layout) {
        super(name, null, layout);
    }

    @Override
    public void append(LogEvent event) {
        Serializable serializable = getLayout().toSerializable(event);
        String message;
        if (serializable instanceof String) {
            message = (String) serializable;
        } else {
            message = "[" + event.getLevel().name() + "]: " + event.getMessage().getFormattedMessage();
        }

        // TODO call forge logger here
        System.out.println("test: " + message);
    }

    @PluginFactory
    public static ClientAppender createAppender(
            @Required(message = "No name provided for ClientAppender") @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout) {

        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }

        return new ClientAppender(name, layout);
    }
}
