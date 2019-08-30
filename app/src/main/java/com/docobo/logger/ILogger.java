/*
 *  Copyright (C) 2019 Docobo Ltd - All Rights Reserved
 *
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 */

package com.docobo.logger;

import java.io.File;

public interface ILogger
{
    void onLog(LogEntry logEntry);
    
    void onLogEmail(String subject, String message, File[] attachFiles);
    
    File createTempFileForData(String data);
}
