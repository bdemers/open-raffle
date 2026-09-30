package org.openraffle.ui.admin;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.openraffle.domain.Participant;
import org.openraffle.domain.Prize;
import org.openraffle.security.SecurityConfig;
import org.openraffle.service.ParticipantService;
import org.openraffle.ui.MainLayout;

/** Type a drawn ticket number and see who holds it and what they want. */
@Route(value = "draw", layout = MainLayout.class)
@PageTitle("Draw | Open Raffle")
@RolesAllowed(SecurityConfig.ROLE_ADMIN)
public class DrawView extends VerticalLayout {

    private final ParticipantService participantService;
    private final Div result = new Div();

    public DrawView(ParticipantService participantService) {
        this.participantService = participantService;
        setMaxWidth("720px");

        IntegerField ticket = new IntegerField("Drawn ticket #");
        ticket.setMin(0);
        ticket.setAutofocus(true);
        Button lookup = new Button("Look up", VaadinIcon.SEARCH.create(), e -> lookup(ticket.getValue()));
        lookup.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        lookup.addClickShortcut(Key.ENTER);

        HorizontalLayout form = new HorizontalLayout(ticket, lookup);
        form.setAlignItems(Alignment.END);

        add(new H2("Draw a winner"), form, result);
    }

    private void lookup(Integer ticketNumber) {
        result.removeAll();
        if (ticketNumber == null) {
            return;
        }
        participantService.findByTicket(ticketNumber).ifPresentOrElse(this::showWinner, () -> {
            Span none = new Span("No participant holds ticket " + ticketNumber + ".");
            none.addClassNames(LumoUtility.TextColor.ERROR);
            result.add(none);
        });
    }

    private void showWinner(Participant p) {
        H3 name = new H3("🎉 " + p.getName());
        Paragraph range = new Paragraph("Holds tickets " + p.getTicketRangeLabel());
        range.addClassNames(LumoUtility.TextColor.SECONDARY);
        result.add(name, range);

        if (p.getWishlist().isEmpty()) {
            result.add(new Paragraph("They have not submitted a wishlist yet."));
        } else {
            OrderedList list = new OrderedList();
            for (Prize prize : p.getWishlist()) {
                list.add(new com.vaadin.flow.component.html.ListItem(prize.getName()));
            }
            result.add(new Paragraph("Prize preferences, most wanted first:"), list);
        }
        if (p.getNotes() != null) {
            Paragraph notes = new Paragraph("Notes: " + p.getNotes());
            notes.getStyle().set("font-style", "italic");
            result.add(notes);
        }
    }
}
