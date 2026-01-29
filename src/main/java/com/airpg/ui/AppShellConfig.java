package com.airpg.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

/**
 * Application shell configuration for Vaadin.
 * Configures Push support for streaming AI responses.
 */
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET_XHR)
public class AppShellConfig implements AppShellConfigurator {
}
