package org.openraffle.ui.admin;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import org.openraffle.domain.Event;
import org.openraffle.service.EventService;
import org.openraffle.ui.events.EventsView;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

/** Resolves the {@code eventId} route parameter for the views that live inside an event. */
final class EventScopedView {

    private EventScopedView() {
    }

    /**
     * The event from the URL if the current user may work in it; otherwise sends them back
     * to the event list with a notice and returns empty.
     */
    static Optional<Event> resolve(BeforeEnterEvent event, EventService eventService) {
        try {
            Long id = event.getRouteParameters().getLong("eventId").orElseThrow(() -> new AccessDeniedException("No event"));
            return Optional.of(eventService.requireAccess(id));
        } catch (AccessDeniedException e) {
            event.forwardTo(EventsView.class);
            Notification.show("That event isn't available to you.", 4000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return Optional.empty();
        }
    }
}
