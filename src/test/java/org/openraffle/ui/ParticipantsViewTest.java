package org.openraffle.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;
import org.openraffle.domain.Event;
import org.openraffle.domain.Participant;
import org.openraffle.domain.Prize;
import org.openraffle.ui.admin.ParticipantsView;
import org.openraffle.ui.events.EventsView;

import java.util.List;

import static com.github.mvysny.kaributesting.v10.GridKt._getCellComponent;
import static com.github.mvysny.kaributesting.v10.GridKt._getFormattedRow;
import static com.github.mvysny.kaributesting.v10.GridKt._size;
import static com.github.mvysny.kaributesting.v10.LocatorJ._assertNoDialogs;
import static com.github.mvysny.kaributesting.v10.LocatorJ._assertNone;
import static com.github.mvysny.kaributesting.v10.LocatorJ._assertOne;
import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static com.github.mvysny.kaributesting.v10.LocatorJ._setValue;
import static com.github.mvysny.kaributesting.v10.NotificationsKt.expectNotifications;
import static org.assertj.core.api.Assertions.assertThat;

class ParticipantsViewTest extends KaribuTest {

    @SuppressWarnings("unchecked")
    private static Grid<Participant> grid() {
        return _get(Grid.class);
    }

    private Event openEventAsOrganizer() {
        Event fair = event("Spring fair", "pat@example.com");
        loginAsOrganizer("pat@example.com");
        start();
        navigate("events/" + fair.getId());
        _assertOne(ParticipantsView.class);
        return fair;
    }

    @Test
    void addingAParticipantRequiresAPhoneAndThenShowsTheQrCode() {
        openEventAsOrganizer();

        _click(_get(Button.class, spec -> spec.withText("Add participant")));
        _setValue(_get(TextField.class, spec -> spec.withLabel("Name")), "Ann");
        _setValue(_get(IntegerField.class, spec -> spec.withLabel("First ticket #")), 100);
        _setValue(_get(IntegerField.class, spec -> spec.withLabel("Last ticket #")), 104);
        _click(_get(Button.class, spec -> spec.withText("Create & show QR")));

        TextField phone = _get(TextField.class, spec -> spec.withLabel("Phone"));
        assertThat(phone.isInvalid()).isTrue();
        assertThat(participants.count()).isZero();

        _setValue(phone, "+44 20 7946 0958");
        _click(_get(Button.class, spec -> spec.withText("Create & show QR")));

        // The editor closes and the QR dialog for the new participant opens.
        Dialog qr = _get(Dialog.class, spec -> spec.withPredicate(d -> "Ann".equals(d.getHeaderTitle())));
        assertThat(qr.isOpened()).isTrue();
        _click(_get(Button.class, spec -> spec.withText("Close")));
        _assertNoDialogs();

        assertThat(_size(grid())).isEqualTo(1);
        assertThat(_getFormattedRow(grid(), 0)).contains("100 – 104").doesNotContain("+44 20 7946 0958");
        Participant ann = participants.findAll().get(0);
        assertThat(ann.getPhone()).isEqualTo("+44 20 7946 0958");
        assertThat(ann.getToken()).isNotBlank();
    }

    @Test
    void overlappingTicketsAreReportedNotSaved() {
        Event fair = openEventAsOrganizer();
        participant(fair, "Ann", 1, 10);
        navigate("events/" + fair.getId() + "/prizes");
        navigate("events/" + fair.getId());

        _click(_get(Button.class, spec -> spec.withText("Add participant")));
        _setValue(_get(TextField.class, spec -> spec.withLabel("Name")), "Bob");
        _setValue(_get(TextField.class, spec -> spec.withLabel("Phone")), "555-0101");
        _setValue(_get(IntegerField.class, spec -> spec.withLabel("First ticket #")), 5);
        _setValue(_get(IntegerField.class, spec -> spec.withLabel("Last ticket #")), 15);
        _click(_get(Button.class, spec -> spec.withText("Create & show QR")));

        assertThat(participants.count()).isEqualTo(1);
        assertThat(_find(Dialog.class)).isNotEmpty(); // editor stays open
        assertThat(com.github.mvysny.kaributesting.v10.NotificationsKt.getNotifications())
                .anyMatch(n -> n.getElement().getTextRecursively().contains("Ann (1 – 10)")
                        || n.getElement().getProperty("text", "").contains("Ann (1 – 10)"));
    }

    @Test
    void nameOpensTheEditorAndWishlistOpensTheRankedList() {
        Event fair = openEventAsOrganizer();
        Prize bike = prize(fair, "Bike");
        Prize book = prize(fair, "Book");
        Participant ann = participant(fair, "Ann", 1, 10);
        ann.setWishlist(List.of(book, bike));
        participants.save(ann);
        navigate("events/" + fair.getId() + "/prizes");
        navigate("events/" + fair.getId());

        _click((Button) _getCellComponent(grid(), 0, "name"));
        Dialog editor = _get(Dialog.class);
        assertThat(editor.getHeaderTitle()).isEqualTo("Edit participant");
        assertThat(_get(TextField.class, spec -> spec.withLabel("Name")).getValue()).isEqualTo("Ann");
        _click(_get(Button.class, spec -> spec.withText("Cancel")));
        _assertNoDialogs();

        Button wishlist = (Button) _getCellComponent(grid(), 0, "wishlist");
        assertThat(wishlist.getText()).isEqualTo("Book › Bike");
        _click(wishlist);
        Dialog picks = _get(Dialog.class);
        assertThat(picks.getHeaderTitle()).isEqualTo("Ann's picks");
        assertThat(_find(ListItem.class)).extracting(li -> li.getElement().getTextRecursively())
                .containsExactly("Book", "Bike");
    }

    @Test
    void deletingAParticipantAsksFirst() {
        Event fair = openEventAsOrganizer();
        participant(fair, "Ann", 1, 10);
        navigate("events/" + fair.getId() + "/prizes");
        navigate("events/" + fair.getId());

        HorizontalLayout actions = (HorizontalLayout) _getCellComponent(grid(), 0, "actions");
        _click((Button) actions.getComponentAt(2));
        confirm(_get(ConfirmDialog.class));

        assertThat(participants.count()).isZero();
        assertThat(_size(grid())).isZero();
        expectNotifications("Participant deleted");
    }

    @Test
    void organizersNotListedOnTheEventAreSentBackToTheList() {
        Event fair = event("Spring fair", "other@example.com");
        loginAsOrganizer("pat@example.com");
        start();

        navigate("events/" + fair.getId());

        _assertNone(ParticipantsView.class);
        _assertOne(EventsView.class);
        expectNotifications("That event isn't available to you.");
    }

    @Test
    void adminsCanOpenAnyEvent() {
        Event fair = event("Spring fair", "other@example.com");
        loginAsAdmin();
        start();

        navigate("events/" + fair.getId());

        _assertOne(ParticipantsView.class);
    }
}
