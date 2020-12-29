package me.minidigger.falcunnetworking.test;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
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
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import me.minidigger.falcunnetworking.common.Constants;

@Plugin(name = "TestAppender", category = "Core", elementType = Appender.ELEMENT_TYPE, printObject = true)
public class TestAppender extends AbstractAppender {
    private static final Queue<String> messages = new ConcurrentLinkedQueue<>();

    public TestAppender(String name, Layout<?> layout) {
        super(name, null, layout);
    }

    @Override
    public void append(LogEvent event) {
        if (!Constants.TEST_MODE) return;
        messages.add("[" + event.getLevel().name() + "]: " + event.getMessage().getFormattedMessage());
    }

    public static Queue<String> getMessages() {
        return messages;
    }

    @PluginFactory
    public static TestAppender createAppender(
            @Required(message = "No name provided for TerminalConsoleAppender") @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout) {

        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }

        return new TestAppender(name, layout);
    }
}
