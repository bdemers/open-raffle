package org.openraffle.ui.admin;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.openraffle.domain.Participant;
import org.openraffle.domain.Prize;
import org.openraffle.security.SecurityConfig;
import org.openraffle.service.ParticipantService;
import org.openraffle.service.PrizeService;
import org.openraffle.ui.MainLayout;

import java.util.List;

/**
 * Type a drawn ticket number and see who holds it and what they want. Tick a prize to
 * record that the winner took it; prizes already taken by earlier winners are struck
 * through so the organizer can move straight to the next preference. Prizes that are
 * not on the winner's list (including ones added on the spot) can be given out too.
 */
@Route(value = "draw", layout = MainLayout.class)
@PageTitle("Draw | Open Raffle")
@RolesAllowed(SecurityConfig.ROLE_ADMIN)
public class DrawView extends VerticalLayout {

    private final ParticipantService participantService;
    private final PrizeService prizeService;
    private final Div result = new Div();
    private Integer lastTicket;

    public DrawView(ParticipantService participantService, PrizeService prizeService) {
        this.participantService = participantService;
        this.prizeService = prizeService;
        setMaxWidth("720px");

        IntegerField ticket = new IntegerField("Drawn ticket #");
        ticket.setMin(0);
        ticket.setAutofocus(true);
        Button lookup = new Button("Look up", VaadinIcon.SEARCH.create(), e -> lookup(ticket.getValue()));
        lookup.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        lookup.addClickShortcut(Key.ENTER).listenOn(ticket);

        HorizontalLayout form = new HorizontalLayout(ticket, lookup);
        form.setAlignItems(Alignment.END);

        add(new H2("Draw a winner"), form, result);
    }

    private void lookup(Integer ticketNumber) {
        lastTicket = ticketNumber;
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
        if (p.getPhone() != null) {
            Anchor phone = new Anchor("tel:" + p.getPhone().replaceAll("[^+\\d]", ""), p.getPhone());
            Paragraph phoneLine = new Paragraph(new Span("📞 "), phone);
            result.add(phoneLine);
        }

        boolean listExhausted;
        if (p.getWishlist().isEmpty()) {
            result.add(new Paragraph("They have not submitted a wishlist yet."));
            listExhausted = true;
        } else {
            Div list = prizeList();
            int rank = 1;
            boolean anyAvailable = false;
            for (Prize prize : p.getWishlist()) {
                list.add(prizeRow(rank++, prize, p));
                anyAvailable |= !prize.isClaimed() || prize.isClaimedBy(p);
            }
            result.add(new Paragraph("Prize preferences, most wanted first. Tick the one they take:"), list);
            listExhausted = !anyAvailable;
            if (listExhausted) {
                Span gone = new Span("Everything on their list has already been claimed.");
                gone.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.FontWeight.SEMIBOLD);
                result.add(new Paragraph(gone));
            }
        }
        if (p.getNotes() != null) {
            Paragraph notes = new Paragraph("Notes: " + p.getNotes());
            notes.getStyle().set("font-style", "italic");
            result.add(notes);
        }

        result.add(otherPrizes(p, listExhausted));
    }

    /**
     * Every unclaimed prize that is not on the winner's list, each claimable, plus a
     * field to add a brand-new prize on the spot. Collapsed unless the winner's own
     * list has nothing left to give.
     */
    private Details otherPrizes(Participant winner, boolean open) {
        List<Prize> others = prizeService.findAll().stream()
                .filter(prize -> !prize.isClaimed() && !winner.getWishlist().contains(prize))
                .toList();

        Div list = prizeList();
        if (others.isEmpty()) {
            Span none = new Span("No other prizes are available.");
            none.addClassNames(LumoUtility.TextColor.TERTIARY);
            list.add(none);
        }
        for (Prize prize : others) {
            list.add(prizeRow(0, prize, winner));
        }

        TextField newPrize = new TextField();
        newPrize.setPlaceholder("New prize name");
        newPrize.setWidthFull();
        Button addOnly = new Button("Add", e -> addPrize(newPrize, winner, false));
        addOnly.setTooltipText("Add to the prize list without claiming it");
        Button addAndGive = new Button("Add & give to " + winner.getName(), VaadinIcon.CHECK.create(),
                e -> addPrize(newPrize, winner, true));
        addAndGive.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addAndGive.addClickShortcut(Key.ENTER).listenOn(newPrize);
        HorizontalLayout addRow = new HorizontalLayout(newPrize, addOnly, addAndGive);
        addRow.setWidthFull();
        addRow.setAlignItems(FlexComponent.Alignment.CENTER);
        addRow.expand(newPrize);

        VerticalLayout content = new VerticalLayout(list, addRow);
        content.setPadding(false);
        content.setSpacing(true);

        Details details = new Details("Other available prizes (" + others.size() + ")", content);
        details.addThemeVariants(DetailsVariant.FILLED);
        details.setOpened(open);
        details.setWidthFull();
        return details;
    }

    private void addPrize(TextField nameField, Participant winner, boolean claim) {
        String name = nameField.getValue() == null ? "" : nameField.getValue().trim();
        if (name.isEmpty()) {
            nameField.setInvalid(true);
            nameField.setErrorMessage("Enter a prize name");
            return;
        }
        Prize prize = new Prize();
        prize.setName(name);
        prize = prizeService.save(prize);
        if (claim) {
            prizeService.claim(prize, winner);
            notify(winner.getName() + " takes " + prize.getName(), NotificationVariant.LUMO_SUCCESS);
        } else {
            notify("Added " + prize.getName(), NotificationVariant.LUMO_SUCCESS);
        }
        lookup(lastTicket);
    }

    private static Div prizeList() {
        Div list = new Div();
        list.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Gap.XSMALL);
        return list;
    }

    /** One prize line. {@code rank} is the wishlist position, or 0 for prizes not on the list. */
    private HorizontalLayout prizeRow(int rank, Prize prize, Participant winner) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.addClassNames(LumoUtility.Padding.SMALL, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Background.CONTRAST_5);
        if (rank > 0) {
            Span rankLabel = new Span(rank + ".");
            rankLabel.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.PRIMARY);
            rankLabel.setWidth("1.5em");
            row.add(rankLabel);
        }

        if (prize.isClaimed() && !prize.isClaimedBy(winner)) {
            // Taken by an earlier winner: show it crossed out, with no way to claim it.
            Span label = new Span(prize.getName());
            label.getStyle().set("text-decoration", "line-through");
            label.addClassNames(LumoUtility.TextColor.TERTIARY);
            Span who = new Span("claimed by " + prize.getClaimedBy().getName());
            who.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.TERTIARY);
            Div text = new Div(label, new Div(who));
            row.add(text);
            row.expand(text);
            return row;
        }

        Checkbox claimed = new Checkbox(prize.getName(), prize.isClaimedBy(winner));
        claimed.addValueChangeListener(e -> {
            try {
                if (e.getValue()) {
                    prizeService.claim(prize, winner);
                    notify(winner.getName() + " takes " + prize.getName(), NotificationVariant.LUMO_SUCCESS);
                } else {
                    prizeService.unclaim(prize);
                }
            } catch (IllegalStateException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
            // Re-read so a concurrent claim from another organizer's screen shows correctly.
            lookup(lastTicket);
        });
        row.add(claimed);
        row.expand(claimed);
        return row;
    }

    private static void notify(String text, NotificationVariant variant) {
        Notification.show(text, 4000, Notification.Position.BOTTOM_CENTER).addThemeVariants(variant);
    }
}
