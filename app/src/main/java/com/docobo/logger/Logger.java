/*
 *  Copyright (C) 2019 Docobo Ltd - All Rights Reserved
 *
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 */

package com.docobo.logger;

import android.util.Log;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingQueue;

public class Logger
{
    private static final ILogger DEFAULT_LOGGER = new ILogger()
    {
        @Override
        public void onLog(LogEntry logEntry)
        {
//            if (logEntry.getLogType() == LogType.Error)
//            {
//                System.err.println(logEntry.getPersistentLogMessage());
//            }
//            else
//            {
//                System.out.println(logEntry.getPersistentLogMessage());
//            }
            int logPriority;
            switch (logEntry.getLogType())
            {
                default:
                case Default:
                case Debug:
                    logPriority = Log.DEBUG; break;
                case Error:
                    logPriority = Log.ERROR; break;
                case Info:
                    logPriority = Log.INFO; break;
                case Verbose:
                    logPriority = Log.VERBOSE; break;
                case Warning:
                    logPriority = Log.WARN; break;
            }
            Log.println(logPriority, logEntry.getTag(), logEntry.getMessage());
        }
    
        @Override
        public void onLogEmail(String subject, String message, File[] attachFiles)
        {
            // No implementation
        }
    
        @Override
        public File createTempFileForData(String data)
        {
            // No implementation
            return null;
        }
    };
    
    /**
     * Default verbosity level.
     */
    private static final int DEFAULT_DEBUG_VERBOSITY_LEVEL = 10;
    /**
     * Default log buffer size.
     */
    private static final int DEFAULT_LOGGER_BUFFER_SIZE = 500;
    
    private static final Object lockHandle = new Object();
    
    /**
     * Default application level debug TAG
     */
    protected static String TAG = "DocoboApp";
    /**
     * Logger implementation.
     */
    private static ILogger loggerImpl = DEFAULT_LOGGER;
    /**
     * Logger.loggerOutputBuffer - the com.docobo.common.logger's output buffer (FIFO).
     */
    private static LinkedBlockingQueue<String> loggerOutputBuffer = new LinkedBlockingQueue<String>(DEFAULT_LOGGER_BUFFER_SIZE);
    /**
     * Debug verbosity level.
     */
    private static int debugVerbosityLevel = DEFAULT_DEBUG_VERBOSITY_LEVEL;
    /**
     * Flag used to determine whether an error event should be generated when a Critical Error is reported.
     */
    private static boolean sendErrorEventsOnCriticalErrors = false;
    /**
     * Formatter used to convert the timestamp into a string.
     */
    private static SimpleDateFormat timestampFormatter;
    
    /**
     * Returns the current debug verbosity level.
     * @return
     */
    public static void setDebugVerbosityLevel(int debugVerbosityLevel)
    {
        synchronized (lockHandle)
        {
            Logger.debugVerbosityLevel = debugVerbosityLevel;
        }
    }
    /**
     * Returns the current debug verbosity level.
     * @return
     */
    public static int getDebugVerbosityLevel()
    {
        synchronized (lockHandle)
        {
            return debugVerbosityLevel;
        }
    }
    
    /**
     * Set the flag determining whether error event emails are sent.
     *
     * @param enable
     */
    public static void setSendErrorEventsOnCriticalErrors(boolean enable)
    {
        synchronized(Logger.lockHandle)
        {
            Logger.sendErrorEventsOnCriticalErrors = enable;
        }
    }
    /**
     * @return the sendErrorEventsOnCriticalErrors
     */
    public static boolean isSendErrorEventsOnCriticalErrorsEnabled()
    {
        synchronized(Logger.lockHandle)
        {
            return sendErrorEventsOnCriticalErrors;
        }
    }
    
    
    /*
     * Static initialiser.
     */
    static
    {
        Logger.timestampFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        Logger.timestampFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    /**
     * Initialise method, must be called as soon as the application context is created.
     *
     * @param loggerImpl
     * @throws NullPointerException if loggerImpl is null
     */
    public static void initialise(String applicationTag, ILogger loggerImpl) throws NullPointerException
    {
        if (loggerImpl == null) throw new NullPointerException("loggerImpl cannot be null");
        
        synchronized (lockHandle)
        {
            Logger.TAG = applicationTag;
            Logger.loggerImpl = loggerImpl;
        }
    }
    
    /**
     * Processes a log entry and calls the appropriate Logger implementation methods.
     * @param logType
     * @param tag
     * @param logMessage
     * @param attachments
     * @param sendEmail
     */
    private static void onLogEntry(LogType logType, String tag, String logMessage, File[] attachments, boolean addToDeviceLog, boolean sendEmail)
    {
        Date timeStamp = new Date();
        synchronized (lockHandle)
        {
            if (Logger.loggerImpl == null)
                throw new RuntimeException("Logger not initialised");
            
            LogEntry entry = new LogEntry(logType, timeStamp, tag, logMessage, addToDeviceLog);
            {
                StringBuilder logBufferEntry = new StringBuilder();
                logBufferEntry.append("[").append(Logger.timestampFormatter.format(timeStamp)).append("]");
                logBufferEntry.append("[").append(logType.getLogEventLabel()).append("]");
                logBufferEntry.append("[").append(tag).append("]");
                logBufferEntry.append(" ").append(logMessage);
                entry.setPersistentLogMessage(logBufferEntry.toString());
            }
            
            // Call the onLog to process this log entry.
            Logger.loggerImpl.onLog(entry);
            /*
             * Pre-pend the time stamp and insert the item into the queue.
             */
            Logger.loggerOutputBuffer.offer(entry.getPersistentLogMessage());
            
            if (sendEmail && isSendErrorEventsOnCriticalErrorsEnabled())
            {
                Logger.loggerImpl.onLogEmail(tag, logMessage, attachments);
            }
            
			/*
			 * Inform all listeners that the the log text has changed.
			 */
            synchronized(Logger.logListeners)
            {
                if (Logger.logListeners.size()  > 0)
                {
                    for(LoggerEventsListener logListener: Logger.logListeners)
                    {
                        logListener.onNewLogEntry(entry);
                    }
                }
            }
        }
    }
    
    public static String getStackTraceString(Throwable tr)
    {
        if (tr == null) {
            return "";
        }
    
        // This is to reduce the amount of log spew that apps do in the non-error
        // condition of the network being unavailable.
        Throwable t = tr;
        while (t != null) {
            if (t instanceof UnknownHostException) {
                return "";
            }
            t = t.getCause();
        }
    
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, false);
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
    
    /**
     * Returns the formatted log message.
     * @param message
     * @param args
     * @return
     */
    private static String getLogMessage(String message, Object... args)
    {
        String logMessage;
        try
        {
            if (args != null && args.length > 0)
            {
                logMessage = String.format(Locale.US, message, args);
            }
            else
            {
                logMessage = message;
            }
        }
        catch (Exception e)
        {
            StringBuilder sb = new StringBuilder(message);
            sb.append(" Args: {");
            for (int index = 0; index < args.length; index++)
            {
                if (index > 0)
                    sb.append(", ");
                sb.append(args[index]);
            }
            sb.append(" }");
            logMessage = sb.toString();
        }
        return logMessage;
    }
    
    /*
     *****************************************************************
     *
     *
     * interface LoggerEventsListener
     *
     *  Class associated with com.docobo.common.logger events.
     *
     ****************************************************************
     */
    
    /**
     * LogListener interface - listen for com.docobo.common.logger events.
     *
     */
    public interface LoggerEventsListener
    {
        public void onNewLogEntry(LogEntry logEntry);
        public void onLogCleared();
    }
    /**
     * static LogListeners Array of log listeners.
     */
    private static ArrayList<LoggerEventsListener> logListeners = new ArrayList<LoggerEventsListener>();
    /**
     * AddListener(LogListener newListener)
     * @param newListener - the new listener to be added to the array of log listeners.
     * @return - True if the item is added, false otherwise.
     */
    public static boolean addListener(LoggerEventsListener newListener)
    {
        boolean result = false;
        if (newListener != null)
        {
            synchronized(Logger.logListeners)
            {
                if (!Logger.logListeners.contains(newListener))
                {
                    Logger.logListeners.add(newListener);
                    result = true;
                }
            }
        }
        return result;
    }
    /**
     * RemoveListener(LogListener existingListener)
     * @param existingListener - the new listener to be removed from the array of log listeners.
     * @return - True if the item is removed, false otherwise.
     */
    public static boolean removeListener(LoggerEventsListener existingListener)
    {
        boolean result = false;
        if (existingListener != null)
        {
            synchronized(Logger.logListeners)
            {
                if (Logger.logListeners.contains(existingListener))
                {
                    Logger.logListeners.remove(existingListener);
                    result = true;
                }
            }
        }
        return result;
    }
    
    /**
     * Returns the log entries in the buffer in the current order.
     *
     * @return - Entries from the com.docobo.common.logger's output buffer
     */
    public static ArrayList<String> getOutputBufferEntries()
    {
        synchronized(Logger.loggerOutputBuffer)
        {
            ArrayList<String> entries = null;
            if (Logger.loggerOutputBuffer.size() > 0)
            {
                entries = new ArrayList<String>(Logger.loggerOutputBuffer);
            }
    
            return entries;
        }
    }
    
    /**
     * clear()
     */
    public static void clear()
    {
		/*
		 * Clear the output buffer.
		 */
        synchronized(Logger.loggerOutputBuffer)
        {
            Logger.loggerOutputBuffer.clear();
        }
		
		/*
		 * Communicate log cleared to all listeners.
		 */
        synchronized(Logger.logListeners)
        {
            if (Logger.logListeners.size()  > 0)
            {
                for(LoggerEventsListener logListener: Logger.logListeners)
                {
                    logListener.onLogCleared();
                }
            }
        }
    }
    
    /*
     ****************************************************************
     *
     *          Log functions
     *
     *****************************************************************
     */
    
    public static void v(String tag, String message, Object... args)
    {
        onLogEntry(LogType.Verbose, tag, getLogMessage(message, args), null, true, false);
    }
    
    public static void d(String tag, String message, Object... args)
    {
        onLogEntry(LogType.Debug, tag, getLogMessage(message, args), null, true, false);
    }
    
    public static void d(String tag, boolean addToDeviceLog, String message, Object... args)
    {
        onLogEntry(LogType.Debug, tag, getLogMessage(message, args), null, addToDeviceLog, false);
    }
    
    public static void df(String tag, int level, String message, Object... args)
    {
        if (level <= getDebugVerbosityLevel())
        {
            onLogEntry(LogType.Debug, tag, getLogMessage(message, args), null, true, false);
        }
    }
    
    public static void i(String tag, String message, Object... args)
    {
        onLogEntry(LogType.Info, tag, getLogMessage(message, args), null, true, false);
    }
    
    public static void w(String tag, String message, Object... args)
    {
        onLogEntry(LogType.Warning, tag, getLogMessage(message, args), null, true, false);
    }
    
    public static void e(String tag, String message, Object... args)
    {
        onLogEntry(LogType.Error, tag, getLogMessage(message, args), null, true, false);
    }
    
    public static void ex(String tag, String message, Throwable e, Object... args)
    {
        onLogEntry(LogType.Error, tag, getLogMessage((message + " - " + getStackTraceString(e)), args), null, true, false);
    }
    
    /**
     * Logs a message with Error logging level, and generates a critical error event if enabled.
     * @param tag
     * @param message - Clear text message to be included in the email body (Should not contain any personal data)
     */
    public static void criticalError(String tag, String message)
    {
        onLogEntry(LogType.Error, tag, message, null, true, true);
    }
    
    /**
     * Logs a message with Error logging level, and generates a critical error event if enabled.
     * @param tag
     * @param message - Clear text message to be included in the email body (Should not contain any personal data)
     * @param attachmentData - Data string that will be included as an encrypted attachment.
     */
    public static void criticalError(String tag, String message, String attachmentData)
    {
        Logger.criticalError(tag, message, attachmentData != null ? new String[] { attachmentData } : null);
    }
    
    /**
     * Logs a message with Error logging level, and generates a critical error event if enabled.
     * @param tag
     * @param message - Clear text message to be included in the email body (Should not contain any personal data)
     * @param attachmentData - Data string that will be included as an encrypted attachment.
     */
    public static void criticalError(String tag, String message, String[] attachmentData)
    {
        synchronized (lockHandle)
        {
            /*
             * Create a temp file containing the attachment data.
             */
            ArrayList<File> attachmentsTemp = new ArrayList<File>();
            File[] attachments = null;
            if (attachmentData != null)
            {
                for (String data : attachmentData)
                {
                    File attachment = Logger.loggerImpl.createTempFileForData(data);
                    if (attachment != null)
                    {
                        attachmentsTemp.add(attachment);
                    }
                }
                
                if (attachmentsTemp.size() > 0)
                {
                    attachments = attachmentsTemp.toArray(new File[attachmentsTemp.size()]);
                }
            }
            onLogEntry(LogType.Error, tag, message, attachments, true, true);
            
            /*
             * Delete the temp file created for the data.
             */
            for (File tempFile : attachmentsTemp)
            {
                tempFile.delete();
            }
        }
    }
    
    /**
     * Logs a message with Error logging level, and generates a critical error event if enabled.
     * @param tag
     * @param message - Clear text message to be included in the email body (Should not contain any personal data)
     * @param attachFile - Data file that will be included as an encrypted attachment.
     */
    public static void criticalError(String tag, String message, File attachFile)
    {
        onLogEntry(LogType.Error, tag, message, (attachFile != null ? new File[] { attachFile } : null), true, true);
    }
    
    /**
     * Logs a message with Error logging level, and generates a critical error event if enabled.
     * 
     * @param tag
     * @param message - Clear text message to be included in the email body (Should not contain any personal data)
     * @param attachFiles - Data files that will be included as an encrypted attachment.
     */
    public static void criticalError(String tag, String message, File[] attachFiles)
    {
        onLogEntry(LogType.Error, tag, message, attachFiles, true, true);
    }
    
    /*
     *****************************************************************
     **
     ** Handler methods.
     **
     *****************************************************************/
    public static void error(String tag, String format, Object... args)
    {
        Logger.e(tag, format, args);
    }
    public static void exception(String tag, String message, Throwable trace)
    {
        Logger.ex(tag, message, trace);
    }
    public static void Error(String format, Object... args)
    {
        Logger.e(TAG, format, args);
    }
    public static void exception(String format, Throwable trace)
    {
        Logger.ex(TAG, format, trace);
    }
    
    public static void warning(String tag, String format, Object... args)
    {
        Logger.w(tag, format, args);
    }
    public static void Warning(String format, Object... args)
    {
        Logger.w(TAG, format, args);
    }
    
    public static void Log(String format, Object... args)
    {
        Logger.i(TAG, format, args);
    }
    
    public static void debug(boolean addToLogcat, String tag, String format, Object... args)
    {
        Logger.d(tag, addToLogcat, format, args);
    }
    public static void debug(String tag, String format, Object... args)
    {
        Logger.d(tag, true, format, args);
    }
    public static void Debug(String format, Object... args)
    {
        Logger.d(TAG, true, format, args);
    }
}
