package org.openraffle.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.openraffle.ui.admin.DrawView;
import org.openraffle.ui.admin.ParticipantsView;
import org.openraffle.ui.admin.PrizesView;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/** Shell for the admin (organizer) views. */
public class MainLayout extends AppLayout {

    public MainLayout(AuthenticationContext auth) {
        H1 title = new H1("Open Raffle");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        String userName = auth.getAuthenticatedUser(OidcUser.class)
                .map(u -> u.getFullName() != null ? u.getFullName() : u.getPreferredUsername())
                .orElse("");
        Span user = new Span(userName);
        user.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        Button logout = new Button("Log out", VaadinIcon.SIGN_OUT.create(), e -> auth.logout());
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title, user, logout);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM);
        addToNavbar(header);

        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Participants", ParticipantsView.class, VaadinIcon.USERS.create()));
        nav.addItem(new SideNavItem("Prizes", PrizesView.class, VaadinIcon.GIFT.create()));
        nav.addItem(new SideNavItem("Draw", DrawView.class, VaadinIcon.TROPHY.create()));
        addToDrawer(nav);
    }
}
