/*
 *  Copyright (C) 2019 Docobo Ltd - All Rights Reserved
 *
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 */

package com.docobo.logger;

public enum LogType
{
    Default,
    Verbose,
    Debug,
    Info,
    Warning,
    Error,
    ;
    
    /**
     * Returns the 4 character log event label that can be used to represent this LogType.
     *
     * @return
     */
    public String getLogEventLabel()
    {
        switch (this)
        {
            case Default:
            default:
            {
                return "DFLT";
            }
            case Verbose:
            {
                return "VERB";
            }
            case Debug:
            {
                return "DBUG";
            }
            case Info:
            {
                return "INFO";
            }
            case Error:
            {
                return "ERROR";
            }
            case Warning:
            {
                return "WARN";
            }
        }
    }
}
