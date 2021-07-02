package org.openinfralabs.caerus.eventListenerService.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@ApiModel(description = "Simplified version of org.apache.hadoop.hdfs.inotify.Event, the Nifi only returns 3 parameter json.")
public class HDFSiNotifyEvent implements Serializable {

    @ApiModelProperty(notes = "Six event types: append, close, create, metadata, rename, and unlink.")
    private String eventType = null;
    @ApiModelProperty(notes = "The path of notify event.")
    private String path = null;
    @ApiModelProperty(notes = "The time of the changes.")
    private Date timeStamp;


    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }



}
