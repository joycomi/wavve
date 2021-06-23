package video;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Video_table")
public class Video {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Integer videoId;
    private String title;
    private String status;

    @PrePersist
    public void onPostPersist(){
        //비디오 정보 등록
        VideoInfoRegistered videoInfoRegistered = new VideoInfoRegistered();
        this.setStatus("Resistered");
        BeanUtils.copyProperties(this, videoInfoRegistered);
        videoInfoRegistered.publishAfterCommit();
    }

    @PostUpdate
    public void onPostUpdate(){
        System.out.println("\n\n##### listener Video-PostUpdate : " + this.getVideoId().toString() + ": "+this.getStatus().toString() + "\n\n");

        StatusModified statusModified = new StatusModified();
        BeanUtils.copyProperties(this, statusModified);
        statusModified.publishAfterCommit();


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
