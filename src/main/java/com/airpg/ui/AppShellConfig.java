package com.airpg.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.theme.Theme;

/**
 * Application shell configuration for Vaadin.
 * Configures Push support for streaming AI responses.
 */
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET_XHR)
@Theme("rpgvibe")
public class AppShellConfig implements AppShellConfigurator {
}
