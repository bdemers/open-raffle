package org.openraffle.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility;

/** Global footer, shown on every page. */
public class AppFooter extends Footer {

    public AppFooter(AppVersion version) {
        Anchor author = new Anchor("https://github.com/dogeared", "dogeared");
        author.setTarget("_blank");
        add(new Span("made with ❤️ by "), author, new Span(" · version " + version.get()));
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.CENTER, LumoUtility.Gap.XSMALL,
                LumoUtility.FontSize.SMALL, LumoUtility.TextColor.TERTIARY, LumoUtility.Padding.MEDIUM,
                LumoUtility.Margin.Top.AUTO);
        setWidthFull();
    }
}
