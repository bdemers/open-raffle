package org.openraffle.ui.pub;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.openraffle.security.CurrentUser;
import org.openraffle.ui.AppFooter;
import org.openraffle.ui.AppVersion;
import org.openraffle.ui.events.EventsView;

/** Public front page: what the app does, and the way in for organizers. */
@Route("")
@PageTitle("Open Raffle")
@AnonymousAllowed
public class LandingView extends VerticalLayout {

    public LandingView(CurrentUser currentUser, AppVersion version) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        VerticalLayout hero = new VerticalLayout();
        hero.setMaxWidth("720px");
        hero.addClassNames(LumoUtility.Margin.Horizontal.AUTO, LumoUtility.Padding.LARGE, LumoUtility.Gap.MEDIUM);

        H1 title = new H1("🎟️ Open Raffle");
        Paragraph tagline = new Paragraph("Run a physical-ticket raffle without the paper chaos.");
        tagline.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.TextColor.SECONDARY);

        Div how = new Div(
                step(VaadinIcon.TICKET, "Register tickets",
                        "Organizers record who holds which ticket numbers as they are sold."),
                step(VaadinIcon.QRCODE, "Scan to pick prizes",
                        "Every participant gets a QR code that opens a page where they rank the prizes they would like. No account needed."),
                step(VaadinIcon.TROPHY, "Draw",
                        "Type the drawn ticket number: see the winner, their phone number and their ranked picks, and tick the prize they take. Prizes already gone are crossed out."));
        how.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Gap.MEDIUM);

        Div actions = new Div();
        actions.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.MEDIUM, LumoUtility.AlignItems.CENTER);
        if (currentUser.isOrganizer()) {
            Button events = new Button("Your events", VaadinIcon.CALENDAR.create(),
                    e -> getUI().ifPresent(ui -> ui.navigate(EventsView.class)));
            events.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
            Span who = new Span("Signed in as " + currentUser.displayName());
            who.addClassNames(LumoUtility.TextColor.SECONDARY);
            actions.add(events, who);
        } else {
            Anchor login = new Anchor("/oauth2/authorization/keycloak", "");
            login.getElement().setAttribute("router-ignore", true);
            Button button = new Button("Organizer log in", VaadinIcon.SIGN_IN.create());
            button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
            login.add(button);
            Span hint = new Span("Participants don't log in — just scan your QR code.");
            hint.addClassNames(LumoUtility.TextColor.SECONDARY);
            actions.add(login, hint);
        }

        Anchor source = new Anchor("https://github.com/dogeared/open-raffle", "Open source on GitHub");
        source.setTarget("_blank");
        source.addClassNames(LumoUtility.FontSize.SMALL);

        hero.add(title, tagline, new H2("How it works"), how, actions, source);
        add(hero, new AppFooter(version));
    }

    private static Div step(VaadinIcon icon, String heading, String text) {
        Span head = new Span(heading);
        head.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
        Paragraph body = new Paragraph(text);
        body.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextColor.SECONDARY);
        Div textCol = new Div(head, body);
        Div row = new Div(icon.create(), textCol);
        row.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.MEDIUM, LumoUtility.AlignItems.START,
                LumoUtility.Padding.MEDIUM, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Background.CONTRAST_5);
        return row;
    }
}
