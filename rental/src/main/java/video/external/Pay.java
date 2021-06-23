package video.external;

public class Pay {

    private Integer payId;
    private Integer price;
    private String payStatus;
    private Integer rentId;
    private Integer videoId;

    public Integer getPayId() {
        return payId;
    }
    public void setPayId(Integer payId) {
        this.payId = payId;
    }
    public Integer getPrice() {
        return price;
    }
    public void setPrice(Integer price) {
        this.price = price;
    }
    public String getPayStatus() {
        return payStatus;
    }
    public void setPayStatus(String payStatus) {
        this.payStatus = payStatus;
    }
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

}
