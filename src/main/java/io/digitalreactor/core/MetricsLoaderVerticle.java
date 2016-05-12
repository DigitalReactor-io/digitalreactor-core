package io.digitalreactor.core;

import io.digitalreactor.core.api.yandex.RequestTable;
import io.digitalreactor.core.api.yandex.YandexApi;
import io.digitalreactor.core.api.yandex.YandexApiImpl;
import io.digitalreactor.core.domain.messages.ReportMessage;
import io.vertx.core.eventbus.EventBus;
import org.joda.time.DateTime;

import java.text.SimpleDateFormat;

/**
 * Created by ingvard on 07.04.16.
 */
public class MetricsLoaderVerticle extends ReactorAbstractVerticle {

    public static final String LOAD_REPORT = "report.loader.load_report";

    private EventBus eventBus;

    private YandexApi yandexApi;

    private final SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-DD");

    @Override
    public void start() throws Exception {
        this.eventBus = vertx.eventBus();
        this.yandexApi = new YandexApiImpl(vertx);
        eventBus.consumer(LOAD_REPORT, result -> {
            createReport(toObj((String) result.body(), ReportMessage.class));
        });
    }

    private void createReport(final ReportMessage reportMessage) {
        DateTime toDay = DateTime.now();
        DateTime before30days = toDay.minusDays(30);

        RequestTable requestTable = RequestTable.of()
                .ids(reportMessage.counterId)
                .date1(sdf.format(before30days))
                .date2(sdf.format(toDay))
                .metrics("ym:s:visits")
                .build();

        yandexApi.tables(requestTable, reportMessage.clientToken, result -> {
            ReportMessage message = new ReportMessage();
            message.summaryId = reportMessage.summaryId;
            message.clientToken = reportMessage.clientToken;
            message.counterId = reportMessage.counterId;
            message.reportType = reportMessage.reportType;
            message.raw = result;

            eventBus.publish(ReportCreatorVerticle.CREATE_REPORT, fromObj(message));
        });

    }
}
