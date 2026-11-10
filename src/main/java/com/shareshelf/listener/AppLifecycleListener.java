package com.shareshelf.listener;

import com.shareshelf.dao.RequestDAO;
import com.shareshelf.util.StaleReservationChecker;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Starts the overdue-checker background thread when the web app starts and stops it cleanly when
 * the app stops (otherwise the thread would keep running after an undeploy).
 */
@WebListener
public class AppLifecycleListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppLifecycleListener.class.getName());

    /** The checker is shared with the Reports page through this ServletContext attribute. */
    public static final String CHECKER_ATTRIBUTE = "overdueChecker";

    private static final int INITIAL_DELAY_SECONDS = 10;

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        int minutes = readMinutes(context.getInitParameter("overdueCheckMinutes"));

        StaleReservationChecker checker = new StaleReservationChecker(new RequestDAO()::markOverdue);
        context.setAttribute(CHECKER_ATTRIBUTE, checker);

        ThreadFactory daemonThreads = runnable -> {
            Thread t = new Thread(runnable, "overdue-checker");
            t.setDaemon(true);   // never blocks the JVM from shutting down
            return t;
        };
        scheduler = Executors.newSingleThreadScheduledExecutor(daemonThreads);
        scheduler.scheduleAtFixedRate(checker, INITIAL_DELAY_SECONDS, minutes * 60L, TimeUnit.SECONDS);
        LOG.info("Overdue checker started: first scan in " + INITIAL_DELAY_SECONDS + "s, then every " + minutes + " min");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (scheduler != null) {
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            LOG.info("Overdue checker stopped");
        }
    }

    private static int readMinutes(String raw) {
        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException | NullPointerException e) {
            return 5;
        }
    }
}
