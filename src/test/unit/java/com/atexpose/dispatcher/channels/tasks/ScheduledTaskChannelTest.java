package com.atexpose.dispatcher.channels.tasks;

import com.atexpose.util.ByteStorage;
import io.schinzel.basicutils.FunnyChars;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.*;


/**
 * @author Schinzel
 */
public class ScheduledTaskChannelTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void constructor_TooLowInterval_Exception() {
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
                new ScheduledTaskChannel("theTaskName", "request", 0)
        );
    }


    @Test
    public void constructor_TooHighInterval_Exception() {
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
                new ScheduledTaskChannel("theTaskName", "request", 1441)
        );
    }


    @Test
    public void constructor_InvalidTimeFormat_Exception() {
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
                new ScheduledTaskChannel("theTaskName", "request", "12345678")
        ).withMessageStartingWith("Incorrect task time: ");
    }


    @Test
    public void constructor_TooLowDayOfMonth_Exception() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> new ScheduledTaskChannel("theTaskName", "request", "23:55", 0))
                .withMessageStartingWith("Incorrect day of month: '0'. Needs to be min 1 or max 28.");
    }


    @Test
    public void constructor_TooHighDayOfMonth_Exception() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> new ScheduledTaskChannel("theTaskName", "request", "23:55", 29))
                .withMessageStartingWith("Incorrect day of month: '29'. Needs to be min 1 or max 28.");
    }


    @Test
    public void getClone_Exception() {
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
                new ScheduledTaskChannel("theTaskName", "request", 15).getClone()
        );
    }


    @Test
    public void writeResponse_DoesNothingButDoesNotThrowException() {
        new ScheduledTaskChannel("theTaskName", "request", 15).writeResponse(null);
    }


    @Test
    public void testRequestReadTime() {
        long actualReadTime = new ScheduledTaskChannel("theTaskName", "request", 15)
                .requestReadTime();
        assertThat(actualReadTime).isEqualTo(0);
    }


    @Test
    public void testSenderInfo() {
        String actualSenderInfo = new ScheduledTaskChannel("TheTaskName", "request", 15)
                .senderInfo();
        assertThat(actualSenderInfo).isEqualTo("ScheduledTask: TheTaskName");
    }


    @Test
    public void testGetRequestAsString() {
        String actualRequest = new ScheduledTaskChannel("The task 1", "ThisIsMyRequest", 55)
                .getRequest();
        assertThat(actualRequest).isEqualTo("ThisIsMyRequest");
    }


    @Test
    public void shutdown_RunningThread_mWasNormalWakeUpTrue() {
        ScheduledTaskChannel stc = new ScheduledTaskChannel("TheTaskName", "ThisIsTheTask", 1);
        Thread thread = new Thread(() -> stc.getRequest(new ByteStorage()));
        thread.start();
        //Interrupt the waiting task
        stc.shutdown(thread);
        //assert that false is returned as the wake-up was not normal, but a shutdown
        assertThat(stc.mShutdownWasInvoked).isTrue();
    }


    @Test
    public void getRequest_ShutdownInvokedWhileWaiting_False() throws InterruptedException {
        ScheduledTaskChannel stc = new ScheduledTaskChannel("TheTaskName", "ThisIsTheTask", 1);
        AtomicBoolean bool = new AtomicBoolean(true);
        Thread thread = new Thread(() -> {
            bool.set(stc.getRequest(new ByteStorage()));
        });
        thread.start();
        new Thread(() -> {
            stc.shutdown(thread);
        }).start();
        thread.join();
        assertThat(bool).isFalse();
    }


    @Test
    public void getRequest_TaskTimeOccurs_True() {
        ScheduledTaskChannel stc = new ScheduledTaskChannel("The task 1", "ThisIsAtask", 1);
        //override next-fire-time and set it to be a short time in the future
        long millisToSleep = 10;
        stc.mTimeToFireNext = LocalDateTime.now(ZoneOffset.UTC).plusNanos(millisToSleep * 1_000_000);
        boolean wasNormalWakeUp = stc.getRequest(new ByteStorage());
        assertThat(wasNormalWakeUp).isTrue();
    }


    @Test
    public void getRequest_TaskTimeOccurs_ByteStorageReturnsSetRequest() {
        String request = FunnyChars.SERBO_CROATION_GAJ.getString();
        ScheduledTaskChannel stc = new ScheduledTaskChannel("The task 1", request, 1);
        //override next-fire-time and set it to be a short time in the future
        long millisToSleep = 10;
        stc.mTimeToFireNext = LocalDateTime.now(ZoneOffset.UTC).plusNanos(millisToSleep * 1_000_000);
        ByteStorage byteStorage = new ByteStorage();
        stc.getRequest(byteStorage);
        assertThat(byteStorage.getAsString()).isEqualTo(request);
    }


    @Test
    public void getRequest_IntervalSetTo15Min_TimeToFireNextIs15Min() {
        ScheduledTaskChannel stc = new ScheduledTaskChannel("The task 1", "TheRequest", 15);
        new Thread(() -> stc.getRequest(new ByteStorage())).start();
        LocalDateTime fifteenMinFromNow = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(15);
        assertThat(stc.mTimeToFireNext).isBetween(fifteenMinFromNow.minusSeconds(1), fifteenMinFromNow.plusSeconds(1));
    }


    @Test
    public void testStatus() {
        ScheduledTaskChannel stc;
        JSONObject status;
        //Test minute interval task
        stc = new ScheduledTaskChannel("The task 1", "time", 55);
        status = stc.getState().getJson();
        assertEquals("The task 1", status.getString("task_name"));
        assertEquals("time", status.getString("request"));
        assertEquals(55, status.getInt("minutes"));
        assertFalse(status.has("time"));
        assertFalse(status.has("day_of_month"));
        assertTrue(status.has("next_task_time_utc"));
        //Test time of day task
        stc = new ScheduledTaskChannel("The task 2", "getStatus", "23:55");
        status = stc.getState().getJson();
        assertEquals("The task 2", status.getString("task_name"));
        assertEquals("getStatus", status.getString("request"));
        assertEquals("23:55", status.getString("time_of_day"));
        assertFalse(status.has("minutes"));
        assertFalse(status.has("day_of_month"));
        assertTrue(status.has("next_task_time_utc"));
        //Test day of month task
        stc = new ScheduledTaskChannel("The task 2", "getStatus", "23:55", 28);
        status = stc.getState().getJson();
        assertEquals("The task 2", status.getString("task_name"));
        assertEquals("getStatus", status.getString("request"));
        assertEquals("23:55", status.getString("time_of_day"));
        assertEquals(28, status.getInt("day_of_month"));
        assertFalse(status.has("minutes"));
        assertTrue(status.has("next_task_time_utc"));
    }


    @Test
    public void test_FireTime_TimeOfDayTask() {
        String taskName = "theTaskName";
        String request = "request";
        String timeOfDay = "08:50";
        ScheduledTaskChannel stc = new ScheduledTaskChannel(taskName, request, timeOfDay);
        assertEquals(1, stc.mIntervalAmount);
        assertEquals(ChronoUnit.DAYS, stc.mIntervalUnit);
        assertEquals(timeOfDay, stc.mTaskTime);
        //Time to fire should be after now
        assertTrue(stc.mTimeToFireNext.isAfter(LocalDateTime.now(ZoneOffset.UTC)));
        //Time to fire should be within 24 hours
        assertTrue(stc.mTimeToFireNext.isBefore(LocalDateTime.now(ZoneOffset.UTC).plus(1, ChronoUnit.DAYS)));
        //
        timeOfDay = "14:55";
        stc = new ScheduledTaskChannel(taskName, request, timeOfDay);
        assertEquals(1, stc.mIntervalAmount);
        assertEquals(ChronoUnit.DAYS, stc.mIntervalUnit);
        assertEquals(timeOfDay, stc.mTaskTime);
        //Time to fire should be after now
        assertTrue(stc.mTimeToFireNext.isAfter(LocalDateTime.now(ZoneOffset.UTC)));
        //Time to fire should be within 24 hours
        assertTrue(stc.mTimeToFireNext.isBefore(LocalDateTime.now(ZoneOffset.UTC).plus(1, ChronoUnit.DAYS)));
        //
        timeOfDay = "21:00";
        stc = new ScheduledTaskChannel(taskName, request, timeOfDay);
        assertEquals(1, stc.mIntervalAmount);
        assertEquals(ChronoUnit.DAYS, stc.mIntervalUnit);
        assertEquals(timeOfDay, stc.mTaskTime);
        //Time to fire should be after now
        assertTrue(stc.mTimeToFireNext.isAfter(LocalDateTime.now(ZoneOffset.UTC)));
        //Time to fire should be within 24 hours
        assertTrue(stc.mTimeToFireNext.isBefore(LocalDateTime.now(ZoneOffset.UTC).plus(1, ChronoUnit.DAYS)));
        //
        timeOfDay = "04:06";
        stc = new ScheduledTaskChannel(taskName, request, timeOfDay);
        assertEquals(1, stc.mIntervalAmount);
        assertEquals(ChronoUnit.DAYS, stc.mIntervalUnit);
        assertEquals(timeOfDay, stc.mTaskTime);
        //Time to fire should be after now
        assertTrue(stc.mTimeToFireNext.isAfter(LocalDateTime.now(ZoneOffset.UTC)));
        //Time to fire should be within 24 hours
        assertTrue(stc.mTimeToFireNext.isBefore(LocalDateTime.now(ZoneOffset.UTC).plus(1, ChronoUnit.DAYS)));
    }


    @Test
    public void test_FireTime_MonthlyTask() {
        //Get day of month 25 days and 2 minutes from now
        LocalDateTime ldtFuture = LocalDateTime.now(ZoneOffset.UTC)
                .plus(25, ChronoUnit.DAYS)
                .plus(2, ChronoUnit.MINUTES);
        while (ldtFuture.getDayOfMonth() > 28) {
            ldtFuture = ldtFuture.minusDays(1);
        }
        //Get the time of the day in the future
        String timeOfDay = DateTimeFormatter.ofPattern("HH:mm").format(ldtFuture);
        //Get the day of month in the future
        int dayOfMonth = ldtFuture.getDayOfMonth();
        //Create the scheduled task
        ScheduledTaskChannel stc = new ScheduledTaskChannel("taskName", "request", timeOfDay, dayOfMonth);
        //Get the number of nanos the task will sleep until it fires
        long sleepTime = stc.getNanosUntilNextTask();
        //Add the sleep time to now
        LocalDateTime ldtWakeUpTime = LocalDateTime.now(ZoneOffset.UTC).plus(sleepTime, ChronoUnit.NANOS);
        //Get the diff between wake-up-time and the future
        long diff = Duration.between(ldtWakeUpTime, ldtFuture).toMillis();
        //Check that the diff between future and wake-up-time is less than 60 seconds
        //There is a diff as the wake-up-time is set to whole minutes
        assertTrue(diff < 60_000);
    }


    @Test
    public void testSleep() {
        String taskName = "theTaskName";
        String request = "request";
        int interval = 15;
        ScheduledTaskChannel stc = new ScheduledTaskChannel(taskName, request, interval);
        long millisToSleep = 20;
        long nanosToSleep = millisToSleep * 1_000_000;
        long start = System.nanoTime();
        stc.sleep(nanosToSleep);
        //Calc the time to do all iterations
        long executionTimeInMS = (System.nanoTime() - start) / 1_000_000;
        assertThat(executionTimeInMS).isBetween(20L, 30L);

    }


}
