package org.openraffle.ui.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.openraffle.domain.Event;
import org.openraffle.domain.Participant;
import org.openraffle.domain.Prize;
import org.openraffle.domain.TicketRange;
import org.openraffle.security.SecurityConfig;
import org.openraffle.service.EventService;
import org.openraffle.service.ParticipantService;
import org.openraffle.service.ParticipantService.TicketRangeConflictException;
import org.openraffle.service.QrCodeService;
import org.openraffle.ui.MainLayout;
import org.openraffle.ui.Paginator;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "events/:eventId", layout = MainLayout.class)
@PageTitle("Participants | Open Raffle")
@RolesAllowed({SecurityConfig.ROLE_ORGANIZER, SecurityConfig.ROLE_ADMIN})
public class ParticipantsView extends VerticalLayout implements BeforeEnterObserver {

    private final ParticipantService participantService;
    private final QrCodeService qrCodeService;
    private final EventService eventService;
    private final Grid<Participant> grid = new Grid<>(Participant.class, false);
    private final Paginator<Participant> pages = new Paginator<>(grid::setItems);
    private Event event;

    public ParticipantsView(ParticipantService participantService, QrCodeService qrCodeService, EventService eventService) {
        this.participantService = participantService;
        this.qrCodeService = qrCodeService;
        this.eventService = eventService;
        setSizeFull();

        Button add = new Button("Add participant", VaadinIcon.PLUS.create(), e -> openEditor(newParticipant()));
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        H2 heading = new H2("Participants");
        HorizontalLayout toolbar = new HorizontalLayout(heading, add);
        toolbar.setAlignItems(Alignment.BASELINE);
        toolbar.expand(heading);
        toolbar.setWidthFull();

        // The name opens the editor, like the pencil button: an extra cue. No phone column:
        // this screen is turned towards participants when they scan their QR code.
        grid.addComponentColumn(p -> {
            Button name = new Button(p.getName(), e -> openEditor(p));
            name.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            return name;
        }).setHeader("Name").setKey("name").setAutoWidth(true);
        // Alphabetical, server-side (the grid only holds one page, so column sorting would mislead).
        grid.addColumn(Participant::getTicketRangeLabel).setHeader("Tickets").setKey("tickets").setAutoWidth(true);
        grid.addColumn(Participant::getTicketCount).setHeader("Count").setAutoWidth(true).setFlexGrow(0);
        // The wishlist summary opens a dialog with the full ranked list.
        grid.addComponentColumn(p -> {
            if (p.getWishlist().isEmpty()) {
                return new Span("—");
            }
            String summary = p.getWishlist().stream().map(Prize::getName).collect(Collectors.joining(" › "));
            Button open = new Button(summary, e -> showWishlist(p));
            open.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            open.setTooltipText("Show " + p.getName() + "'s picks");
            open.getStyle().set("white-space", "normal").set("text-align", "left");
            return open;
        }).setHeader("Wishlist (in order)").setKey("wishlist").setFlexGrow(1);
        grid.addComponentColumn(p -> {
            Button qr = new Button(VaadinIcon.QRCODE.create(), e -> showQr(p));
            qr.setTooltipText("Show QR code");
            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openEditor(p));
            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(p));
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
            HorizontalLayout actions = new HorizontalLayout(qr, edit, delete);
            actions.getChildren().forEach(c -> ((Button) c)
                    .addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL));
            return actions;
        }).setHeader("").setKey("actions").setAutoWidth(true).setFlexGrow(0);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();

        add(toolbar, grid, pages);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent enter) {
        EventScopedView.resolve(enter, eventService).ifPresent(e -> {
            event = e;
            refresh();
        });
    }

    private Participant newParticipant() {
        Participant p = new Participant();
        p.setEvent(event);
        return p;
    }

    private void refresh() {
        pages.setItems(participantService.findAll(event));
    }

    private void openEditor(Participant participant) {
        boolean isNew = participant.getId() == null;
        Dialog dialog = new Dialog(isNew ? "New participant" : "Edit participant");

        TextField name = new TextField("Name");
        TextField phone = new TextField("Phone");
        phone.setPlaceholder("555-123-4567");
        phone.setHelperText("Outside the US, start with + and the country code, e.g. +44 20 7946 0958");
        phone.setMaxLength(32);

        BeanValidationBinder<Participant> binder = new BeanValidationBinder<>(Participant.class);
        binder.forField(name).asRequired("Name is required").bind(Participant::getName, Participant::setName);
        binder.forField(phone).asRequired("Phone is required")
                .withValidator(Participant::isPlausiblePhone, Participant.PHONE_RULE)
                .bind(Participant::getPhone, Participant::setPhone);
        if (!isNew) {
            binder.readBean(participant);
        }

        // One row per ticket range; people come back to buy more, so ranges can be added.
        VerticalLayout rangeRows = new VerticalLayout();
        rangeRows.setPadding(false);
        rangeRows.setSpacing(false);
        Span rangesLabel = new Span("Tickets");
        rangesLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.MEDIUM, LumoUtility.TextColor.SECONDARY);
        Button addRange = new Button("Add another range", VaadinIcon.PLUS.create(), e -> addRangeRow(rangeRows, null).focus());
        addRange.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        if (participant.getRanges().isEmpty()) {
            addRangeRow(rangeRows, null);
        } else {
            participant.getRangesInOrder().forEach(range -> addRangeRow(rangeRows, range));
        }

        FormLayout form = new FormLayout(name, phone);
        dialog.add(form, rangesLabel, rangeRows, addRange);
        dialog.setWidth("520px");

        Button save = new Button(isNew ? "Create & show QR" : "Save", e -> {
            try {
                binder.writeBean(participant);
                List<TicketRange> ranges = readRanges(rangeRows);
                if (ranges == null) {
                    return; // a row is incomplete; its fields are marked
                }
                participant.setRanges(new ArrayList<>(ranges));
                Participant saved = participantService.save(participant);
                dialog.close();
                refresh();
                if (isNew) {
                    showQr(saved);
                }
            } catch (ValidationException ex) {
                // shown inline by binder
            } catch (TicketRangeConflictException ex) {
                Notification n = Notification.show(ex.getMessage(), 6000, Notification.Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage(), 6000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), save);
        dialog.open();
        name.focus();
    }

    /** A "first – last" pair with a remove button; returns the first-ticket field for focusing. */
    private static IntegerField addRangeRow(VerticalLayout rows, TicketRange existing) {
        IntegerField first = new IntegerField("First ticket #");
        IntegerField last = new IntegerField("Last ticket #");
        first.setMin(0);
        last.setMin(0);
        // The last ticket defaults to the first one; select it on focus so typing replaces it.
        last.setAutoselect(true);
        if (existing != null) {
            first.setValue((int) existing.getStart());
            last.setValue((int) existing.getEnd());
        }
        // Prefill in the browser at "change" time (before focus moves on) so autoselect on
        // the last-ticket field highlights the value; the server listener is the fallback.
        first.getElement().executeJs(
                "this.addEventListener('change', () => { const end = $0;"
                        + " if (!end.value && this.value) { end.value = this.value; end.dispatchEvent(new Event('change')); } })",
                last.getElement());
        first.addValueChangeListener(e -> {
            if (e.isFromClient() && last.isEmpty() && e.getValue() != null) {
                last.setValue(e.getValue());
            }
        });

        HorizontalLayout row = new HorizontalLayout(first, last);
        row.setAlignItems(Alignment.END);
        Button remove = new Button(VaadinIcon.CLOSE_SMALL.create(), e -> {
            rows.remove(row);
            updateRemoveButtons(rows);
        });
        remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        remove.setAriaLabel("Remove range");
        remove.setTooltipText("Remove range");
        row.add(remove);
        rows.add(row);
        updateRemoveButtons(rows);
        return first;
    }

    /** The only remaining range cannot be removed. */
    private static void updateRemoveButtons(VerticalLayout rows) {
        long count = rows.getChildren().count();
        rows.getChildren().forEach(row -> ((HorizontalLayout) row).getChildren()
                .filter(Button.class::isInstance).map(Button.class::cast)
                .forEach(b -> b.setEnabled(count > 1)));
    }

    /** The ranges typed into the rows, or null (with the offending fields marked) if a row is incomplete. */
    private static List<TicketRange> readRanges(VerticalLayout rows) {
        List<TicketRange> ranges = new ArrayList<>();
        boolean complete = true;
        for (var child : rows.getChildren().toList()) {
            HorizontalLayout row = (HorizontalLayout) child;
            IntegerField first = (IntegerField) row.getComponentAt(0);
            IntegerField last = (IntegerField) row.getComponentAt(1);
            boolean rowOk = true;
            for (IntegerField field : List.of(first, last)) {
                boolean missing = field.getValue() == null;
                field.setInvalid(missing);
                field.setErrorMessage(missing ? "Required" : null);
                rowOk &= !missing;
            }
            if (rowOk && last.getValue() < first.getValue()) {
                last.setInvalid(true);
                last.setErrorMessage("Must be ≥ first ticket");
                rowOk = false;
            }
            if (rowOk) {
                ranges.add(new TicketRange(first.getValue(), last.getValue()));
            }
            complete &= rowOk;
        }
        return complete ? ranges : null;
    }

    private void showWishlist(Participant participant) {
        Dialog dialog = new Dialog(participant.getName() + "'s picks");
        OrderedList list = new OrderedList();
        for (Prize prize : participant.getWishlist()) {
            ListItem item = new ListItem(prize.getName());
            if (prize.isClaimed()) {
                Span claimed = new Span(prize.isClaimedBy(participant)
                        ? " — they took this one" : " — claimed by " + prize.getClaimedBy().getName());
                claimed.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.TERTIARY);
                item.add(claimed);
            }
            list.add(item);
        }
        Paragraph hint = new Paragraph("Most wanted first, as ranked by " + participant.getName() + ".");
        hint.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        if (participant.getWishlistUpdatedAt() != null) {
            hint.setText(hint.getText() + " Last saved " + participant.getWishlistUpdatedAt() + ".");
        }
        dialog.add(list, hint);
        dialog.setMaxWidth("480px");
        dialog.getFooter().add(new Button("Close", e -> dialog.close()));
        dialog.open();
    }

    private void showQr(Participant participant) {
        Dialog dialog = new Dialog(participant.getName());
        String url = qrCodeService.wishlistUrl(participant);

        StreamResource png = new StreamResource("qr-" + participant.getId() + ".png",
                () -> new ByteArrayInputStream(qrCodeService.pngFor(participant, 512)));
        Image image = new Image(png, "QR code for " + participant.getName());
        image.setWidth("min(70vw, 360px)");
        image.setHeight("min(70vw, 360px)");

        Span tickets = new Span("Tickets " + participant.getTicketRangeLabel());
        tickets.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
        Anchor link = new Anchor(url, url);
        link.setTarget("_blank");
        link.addClassNames(LumoUtility.FontSize.SMALL);
        Paragraph hint = new Paragraph("Scan to choose the prizes you'd like if your ticket is drawn.");
        hint.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        VerticalLayout content = new VerticalLayout(tickets, image, link, hint);
        content.setAlignItems(Alignment.CENTER);
        content.setPadding(false);
        dialog.add(content);

        Anchor download = new Anchor(png, "Download PNG");
        download.getElement().setAttribute("download", true);
        dialog.getFooter().add(download, new Button("Close", e -> dialog.close()));
        dialog.open();
    }

    private void confirmDelete(Participant participant) {
        ConfirmDialog confirm = new ConfirmDialog("Delete participant?",
                participant.getName() + " (tickets " + participant.getTicketRangeLabel()
                        + ") and their wishlist will be removed. Their QR code will stop working.",
                "Delete", e -> {
                    participantService.delete(participant);
                    refresh();
                    Notification.show("Participant deleted");
                }, "Cancel", e -> {
                });
        confirm.setConfirmButtonTheme("error primary");
        confirm.open();
    }
}
