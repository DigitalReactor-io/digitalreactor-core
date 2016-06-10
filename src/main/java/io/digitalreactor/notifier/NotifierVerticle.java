package io.digitalreactor.notifier;

import io.digitalreactor.notifier.application.email.adapter.EmailAdapter;
import io.digitalreactor.notifier.application.email.mapper.NotificationTemplateEmailMapper;
import io.digitalreactor.notifier.domain.Notification;
import io.digitalreactor.notifier.domain.NotificationRouter;
import io.digitalreactor.notifier.domain.Notifier;
import io.digitalreactor.notifier.domain.Route;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;

import java.util.List;

/**
 * Created by ingvard on 03.06.2016.
 */
public class NotifierVerticle extends AbstractVerticle {
    private NotificationRouter notificationRouter;
    private Notifier notifier;

    public NotifierVerticle(NotificationRouter notificationRouter, Notifier notifier) {
        this.notificationRouter = notificationRouter;
        this.notifier = notifier;
    }

    public enum Address {
        NOTIFY;

        public final static String BASE_ADDRESS = "digitalreactor.notifier.";

        public String address() {
            return BASE_ADDRESS + name();
        }
    }

    @Override
    public void start() throws Exception {

        vertx.eventBus().consumer(Address.NOTIFY.address(), this::notificationResolver);
    }

    private void notificationResolver(Message<Notification> message) {
        Notification notification = message.body();

        List<Route> routes = notificationRouter.findRoutesByEvent(notification.eventType());

        for (Route route : routes) {
            notifier.send(route, notification);
        }
    }

    private MailClient mailClient() {
        MailConfig config = new MailConfig();
        config.setHostname(System.getenv("MAIL_SERVER"));
        config.setPort(Integer.valueOf(System.getenv("MAIL_POST")));
        config.setSsl(Boolean.valueOf(System.getenv("MAIL_SSL")));
        config.setUsername(System.getenv("MAIL_USERNAME"));
        config.setPassword(System.getenv("MAIL_PASSWORD"));

        return MailClient.createNonShared(vertx, config);
    }
}