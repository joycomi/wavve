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

    @PostPersist
    public void onPostPersist() throws Exception{
        System.out.println("\n\n##### listener Pay-onPostPersist:paid "+ this.getPayStatus().toString() +" ####\n\n");

        if(this.getPayStatus().matches("OK")){
            Paid paid = new Paid();
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




}
