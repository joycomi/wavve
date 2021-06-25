package video;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Pay_table")
public class Pay {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer payId;
    private Integer price;
    private String payStatus;
    private Integer rentId;
    private Integer videoId;
    //add
    private String videoTitle;
    private String status;
    private String memId;

    @PrePersist
    public void onPostPersist() throws Exception{
        System.out.println("\n\n##### listener Pay-onPostPersist:paid "+ this.getPayStatus().toString() +" ####\n\n");

        if(this.getPayStatus().matches("OK")){
            Paid paid = new Paid();
            this.setStatus("BOOKED");
            BeanUtils.copyProperties(this, paid);
            paid.publishAfterCommit();
        }else{
            throw new Exception("Pay is Not OK Received!!");
        }
    }

    //@PostUpdate
    @PreUpdate
    public void onPostUpdate(){
        System.out.println("\n\n##### listener Pay-onPostUpdate:Refunded "+ this.getRentId().toString()+": "+ this.getPrice().toString() +" ####\n\n");

        Refunded refunded = new Refunded();
        BeanUtils.copyProperties(this, refunded);
        refunded.publishAfterCommit();
    }


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

    //add
    public String getVideoTitle() {
        return videoTitle;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
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
