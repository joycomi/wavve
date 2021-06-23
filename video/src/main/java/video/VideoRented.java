package video;

public class VideoRented extends AbstractEvent {

    private Integer rentId;
    private Integer videoId;
    private String videoTile;
    private Integer rentPrice;
    private String status;
    private String memId;

    public Integer getRentId() {
        return rentId;
    }

    public void setRentId(Integer rentId) {
        this.rentId = rentId;
    }
    public Integer getVideoId() {
        return videoId;
    }

    public void setVideoId(Integer videoId) {
        this.videoId = videoId;
    }
    public String getVideoTile() {
        return videoTile;
    }

    public void setVideoTile(String videoTile) {
        this.videoTile = videoTile;
    }
    public Integer getRentPrice() {
        return rentPrice;
    }

    public void setRentPrice(Integer rentPrice) {
        this.rentPrice = rentPrice;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public String getMemId() {
        return memId;
    }

    public void setMemId(String memId) {
        this.memId = memId;
    }
}