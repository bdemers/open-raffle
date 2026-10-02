package org.openraffle.ui.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.openraffle.domain.Event;
import org.openraffle.domain.Prize;
import org.openraffle.security.SecurityConfig;
import org.openraffle.service.EventService;
import org.openraffle.service.PrizeService;
import org.openraffle.ui.MainLayout;
import org.openraffle.ui.Paginator;


@Route(value = "events/:eventId/prizes", layout = MainLayout.class)
@PageTitle("Prizes | Open Raffle")
@RolesAllowed({SecurityConfig.ROLE_ORGANIZER, SecurityConfig.ROLE_ADMIN})
public class PrizesView extends VerticalLayout implements BeforeEnterObserver {

    private final PrizeService prizeService;
    private final EventService eventService;
    private final Grid<Prize> grid = new Grid<>(Prize.class, false);
    private final Paginator<Prize> pages = new Paginator<>(grid::setItems);
    private Event event;

    public PrizesView(PrizeService prizeService, EventService eventService) {
        this.prizeService = prizeService;
        this.eventService = eventService;
        setSizeFull();

        Button add = new Button("Add prize", VaadinIcon.PLUS.create(), e -> openEditor(newPrize()));
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout toolbar = new HorizontalLayout(new H2("Prizes"), add);
        toolbar.setAlignItems(Alignment.BASELINE);
        toolbar.expand(toolbar.getComponentAt(0));
        toolbar.setWidthFull();

        // Alphabetical; participants rank prizes themselves on their wishlist page.
        grid.addColumn(Prize::getName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(Prize::getDescription).setHeader("Description").setFlexGrow(1);
        grid.addComponentColumn(prize -> {
            if (!prize.isClaimed()) {
                return new Span();
            }
            Span claimed = new Span("Claimed by " + prize.getClaimedBy().getName());
            claimed.getElement().getThemeList().add("badge success");
            return claimed;
        }).setHeader("Status").setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(prize -> {
            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openEditor(prize));
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(prize));
            delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, delete);
        }).setHeader("").setAutoWidth(true).setFlexGrow(0);
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

    private Prize newPrize() {
        Prize prize = new Prize();
        prize.setEvent(event);
        return prize;
    }

    private void refresh() {
        pages.setItems(prizeService.findAll(event));
    }

    private void openEditor(Prize prize) {
        Dialog dialog = new Dialog(prize.getId() == null ? "New prize" : "Edit prize");

        TextField name = new TextField("Name");
        TextArea description = new TextArea("Description");

        BeanValidationBinder<Prize> binder = new BeanValidationBinder<>(Prize.class);
        binder.forField(name).asRequired("Name is required").bind(Prize::getName, Prize::setName);
        binder.forField(description).bind(Prize::getDescription, Prize::setDescription);
        binder.readBean(prize);

        FormLayout form = new FormLayout(name, description);
        form.setColspan(name, 2);
        form.setColspan(description, 2);
        dialog.add(form);

        Button save = new Button("Save", e -> {
            try {
                binder.writeBean(prize);
                prizeService.save(prize);
                dialog.close();
                refresh();
            } catch (ValidationException ex) {
                // field-level errors already shown by the binder
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), save);
        dialog.open();
    }

    private void confirmDelete(Prize prize) {
        ConfirmDialog confirm = new ConfirmDialog("Delete prize?",
                "\"" + prize.getName() + "\" will be removed from every participant's wishlist.",
                "Delete", e -> {
                    prizeService.delete(prize);
                    refresh();
                    Notification.show("Prize deleted");
                }, "Cancel", e -> {
                });
        confirm.setConfirmButtonTheme("error primary");
        confirm.open();
    }
}
