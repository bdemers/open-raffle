package org.openraffle.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import org.junit.jupiter.api.Test;
import org.openraffle.ui.pub.LandingView;

import static com.github.mvysny.kaributesting.v10.LocatorJ._assertNone;
import static com.github.mvysny.kaributesting.v10.LocatorJ._assertOne;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

class LandingViewTest extends KaribuTest {

    @Test
    void anonymousVisitorsGetTheLandingPageWithALoginButton() {
        start();
        navigate("");

        _assertOne(LandingView.class);
        Anchor login = _get(Anchor.class, spec -> spec.withAttribute("href", "/oauth2/authorization/keycloak"));
        assertThat(login.getChildren().filter(Button.class::isInstance).count()).isEqualTo(1);
        _assertOne(Button.class, spec -> spec.withText("Organizer log in"));
        assertThat(_get(AppFooter.class).getElement().getTextRecursively()).contains("made with", "dogeared", "version");
    }

    @Test
    void signedInOrganizersAreOfferedTheirEvents() {
        loginAsOrganizer("pat@example.com");
        start();

        navigate("");

        _assertOne(Button.class, spec -> spec.withText("Your events"));
        _assertNone(Button.class, spec -> spec.withText("Organizer log in"));
    }
}
