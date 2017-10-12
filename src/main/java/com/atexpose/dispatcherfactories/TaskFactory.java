package com.atexpose.dispatcherfactories;

import com.atexpose.dispatcher.Dispatcher;
import com.atexpose.dispatcher.channels.tasks.ScheduledTaskChannel;
import com.atexpose.dispatcher.channels.tasks.ScheduledTaskChannelDaily;
import com.atexpose.dispatcher.channels.tasks.ScheduledTaskChannelMinute;
import com.atexpose.dispatcher.channels.tasks.ScheduledTaskChannelMonthly;
import com.atexpose.dispatcher.logging.Logger;
import com.atexpose.dispatcher.logging.LoggerType;
import com.atexpose.dispatcher.logging.format.LogFormatterFactory;
import com.atexpose.dispatcher.logging.writer.LogWriterFactory;
import com.atexpose.dispatcher.parser.TextParser;
import com.atexpose.dispatcher.wrapper.CsvWrapper;
import lombok.Builder;

public class TaskFactory {


    @Builder(builderMethodName = "minuteTaskBuilder", builderClassName = "MinuteTaskBuilder")
    static Dispatcher newMinuteTask(String taskName, String request, int minutes) {
        ScheduledTaskChannelMinute scheduledTaskChannel = new ScheduledTaskChannelMinute(taskName, request, minutes);
        return TaskFactory.addTask(taskName, scheduledTaskChannel);
    }


    @Builder(builderMethodName = "dailyTaskBuilder", builderClassName = "DailyTaskBuilder")
    static Dispatcher newDailyTask(String taskName, String request, String timeOfDay, String zoneId) {
        ScheduledTaskChannelDaily scheduledTaskChannel = new ScheduledTaskChannelDaily(taskName, request, timeOfDay, zoneId);
        return TaskFactory.addTask(taskName, scheduledTaskChannel);
    }


    @Builder(builderMethodName = "monthlyTaskBuilder", builderClassName = "MonthlyTaskBuilder")
    static Dispatcher newMonthlyTask(String taskName, String request, String timeOfDay, int dayOfMonth, String zoneId) {
        ScheduledTaskChannelMonthly scheduledTaskChannel = new ScheduledTaskChannelMonthly(taskName, request, timeOfDay, dayOfMonth, zoneId);
        return TaskFactory.addTask(taskName, scheduledTaskChannel);
    }


    static Dispatcher addTask(String taskName, ScheduledTaskChannel scheduledTask) {
        Logger errorLogger = Logger.builder()
                .loggerType(LoggerType.ERROR)
                .logFormatter(LogFormatterFactory.JSON.create())
                .logWriter(LogWriterFactory.SYSTEM_OUT.create())
                .build();
        Logger eventLogger = Logger.builder()
                .loggerType(LoggerType.EVENT)
                .logFormatter(LogFormatterFactory.JSON.create())
                .logWriter(LogWriterFactory.SYSTEM_OUT.create())
                .build();
        return Dispatcher.builder()
                .name("ScheduledTask_" + taskName)
                .accessLevel(3)
                .isSynchronized(false)
                .channel(scheduledTask)
                .parser(new TextParser())
                .wrapper(new CsvWrapper())
                .noOfThreads(1)
                .build()
                .addLogger(errorLogger)
                .addLogger(eventLogger);
    }
}
