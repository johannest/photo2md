package org.vaadin.photo2md;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

@PWA(name = "Photo to Markdown", shortName = "photo2md")
@Theme("photo2md")
@Push
public class AppShell implements AppShellConfigurator {
}
