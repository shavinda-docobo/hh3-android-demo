/*
 *  Copyright (C) 2019 Docobo Ltd - All Rights Reserved
 *
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 */

package com.docobo.logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class LogEntry
{
    private final LogType logType;
    private final Date    timestamp;
    private final String  tag;
    private final String  message;
    private final boolean deviceLogEntry;
    
    private String persistentLogMessage;
    
    LogEntry(LogType logType, Date timestamp, String tag, String message, boolean deviceLogEntry)
    {
        this.logType = logType;
        this.timestamp = timestamp;
        this.tag = tag;
        this.message = message;
        this.deviceLogEntry = deviceLogEntry;
    }
    
    public LogType getLogType()
    {
        return logType;
    }
    
    public Date getTimestamp()
    {
        return timestamp;
    }
    
    public String getTag()
    {
        return tag;
    }
    
    public String getMessage()
    {
        return message;
    }
    
    public boolean isDeviceLogEntry()
    {
        return deviceLogEntry;
    }
    
    public void setPersistentLogMessage(String persistentLogMessage)
    {
        this.persistentLogMessage = persistentLogMessage;
    }
    
    public String getPersistentLogMessage()
    {
        return persistentLogMessage;
    }
    
    @Override
    public String toString()
    {
        String timestampString = null;
        if (timestamp != null)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            timestampString = sdf.format(timestamp);
        }
        
        return "LogEntry{" +
                "logType=" + logType +
                ", timestamp=" +  timestampString +
                ", tag='" + tag + '\'' +
                ", message='" + message + '\'' +
                ", deviceLogEntry=" + deviceLogEntry +
                '}';
    }
}
