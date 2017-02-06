package eu.supersede.gr.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "h_alerts")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class HAlert
{
    @Id
    private String id;
    private String applicationId;
    private long timestamp;

    public HAlert()
    {
    }

    public HAlert(String id, long timestamp)
    {
        this.id = id;
        this.timestamp = timestamp;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getApplicationId()
    {
        return applicationId;
    }

    public void setApplicationId(String applicationId)
    {
        this.applicationId = applicationId;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
    }
}