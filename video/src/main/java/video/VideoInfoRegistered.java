package video;

public class VideoInfoRegistered extends AbstractEvent {

    private Integer videoId;
    private String title;
    private String status;

    public VideoInfoRegistered(){
        super();
    }

    public Integer getVideoId() {
        return videoId;
    }

    public void setVideoId(Integer videoId) {
        this.videoId = videoId;
    }
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
