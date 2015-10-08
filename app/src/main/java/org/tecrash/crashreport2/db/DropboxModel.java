package org.tecrash.crashreport2.db;

import android.support.annotation.Nullable;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.NotNull;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.annotation.Unique;
import com.raizlabs.android.dbflow.structure.BaseModel;

/**
 * DB schema of Dropbox table
 * Created by xiaocong on 15/10/3.
 */
@Table(
        tableName = "dropbox",
        databaseName = AppDatabase.NAME
)
public class DropboxModel extends BaseModel {

    @Column(name = "id")
    @PrimaryKey(autoincrement = true)
    protected long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NotNull
    @Column
    protected String tag;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @NotNull
    @Column
    @Unique(unique = true)
    protected long timestamp;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @NotNull
    @Column
    protected String log = "";

    public String getLog() {
        return  log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    @Nullable
    @Column
    protected String serverId;

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public DropboxModel() {
    }

    public DropboxModel(String tag, Long timestamp, String log) {
        this.tag = tag;
        this.timestamp = timestamp;
        this.log = log;
    }

    @Override
    public String toString() {
        return "ID = " + id + ", Tag = " + tag + ", Timestamp = " + timestamp + ", Log = " + log;
    }

}