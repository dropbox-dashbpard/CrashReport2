package org.tecrash.crashreport2.db;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ConflictAction;
import com.raizlabs.android.dbflow.annotation.NotNull;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.annotation.Unique;
import com.raizlabs.android.dbflow.annotation.UniqueGroup;
import com.raizlabs.android.dbflow.structure.BaseModel;

/**
 * DB schema of Dropbox table
 * Created by xiaocong on 15/10/3.
 */
@Table(
    tableName = "dropbox",
    databaseName = AppDatabase.NAME,
    uniqueColumnGroups = @UniqueGroup(groupNumber = 1, uniqueConflict = ConflictAction.FAIL)
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
    @Unique(unique = false, uniqueGroups = 1)
    protected String tag;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @NotNull
    @Column
    @Unique(unique = false, uniqueGroups = 1)
    protected String app;

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
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
    protected long uptime;

    public long getUptime() {
        return uptime;
    }

    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    @NotNull
    @Column
    protected int occurs = 1;

    public int getOccurs() {
        return occurs;
    }

    public void setOccurs(int occurs) {
        this.occurs = occurs;
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

    @NotNull
    @Column
    @Unique(unique = false, uniqueGroups = 1)
    protected String incremental = "";

    public String getIncremental() {
        return  incremental;
    }

    public void setIncremental(String incremental) {
        this.incremental = incremental;
    }

    @Column
    @Unique(unique = false, uniqueGroups = 1)
    protected String serverId;

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    @NotNull
    @Column
    protected int contentUploadStatus = SHOULD_NOT;

    public int getContentUploadStatus() {
        return contentUploadStatus;
    }

    public void setContentUploadStatus(int contentUploadStatus) {
        this.contentUploadStatus = contentUploadStatus;
    }

    @NotNull
    @Column
    protected int logUploadStatus = SHOULD_NOT;

    public int getLogUploadStatus() {
        return logUploadStatus;
    }

    public void setLogUploadStatus(int logUploadStatus) {
        this.logUploadStatus = logUploadStatus;
    }

    public DropboxModel() {
    }

    public DropboxModel(String tag, String app, String incremental, long timestamp, String log, long uptime) {
        this.tag = tag;
        this.app = app;
        this.incremental = incremental;
        this.timestamp = timestamp;
        this.log = log;
        this.uptime = uptime;
        this.occurs = 1;
    }

    @Override
    public String toString() {
        return "ID = " + id + ", Tag = " + tag + ", App = " + app + ", Timestamp = " + timestamp + ", Occurs = " + occurs + ", Log = " + log;
    }

    public final static int SHOULD_NOT = 0;
    public final static int SHOULD_BUT_NOT_UPLOADED = 1;
    public final static int SHOULD_AND_UPLOADED = 3;
}